package com.rfidgateway.reader;

import com.impinj.octane.*;
import com.rfidgateway.model.Antenna;
import com.rfidgateway.model.Reader;
import com.rfidgateway.model.ReaderBrand;
import com.rfidgateway.model.ReaderHardwareInfo;
import com.rfidgateway.model.ReaderOperationMode;
import com.rfidgateway.repository.AntennaRepository;
import com.rfidgateway.repository.ReaderRepository;
import com.rfidgateway.tag.TagEventService;
import com.rfidgateway.tag.WebSocketEventService;
import com.rfidgateway.session.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Component
public class ReaderManager {

    private final Map<String, ImpinjReader> readers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reconnectExecutor = Executors.newScheduledThreadPool(5);
    /** Tareas programadas para auto-detener lectura; se cancelan si el usuario frena antes. */
    private final Map<String, ScheduledFuture<?>> readingAutoStopFutures = new ConcurrentHashMap<>();

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private AntennaRepository antennaRepository;

    @Autowired
    private TagEventService tagEventService;

    @Autowired
    private WebSocketEventService webSocketEventService;

    @Autowired(required = false)
    private SessionService sessionService;

    /** Umbral mínimo RSSI (dBm). Lecturas por debajo se ignoran. Vacío = no filtrar. */
    @Value("${rfid.rssi-min-dbm:}")
    private String rssiMinDbmConfig;

    /** Minutos tras los cuales se detiene la lectura automáticamente (0 = sin límite). */
    @Value("${rfid.reading-timeout-minutes:2}")
    private int readingTimeoutMinutes;

    public boolean isContinuousInventoryReader(String readerId) {
        return readerRepository.findById(readerId)
            .map(r -> r.getOperationMode() == ReaderOperationMode.CONTINUOUS)
            .orElse(false);
    }

    /** Solo lectores con marca Impinj Octane usan el SDK en este gateway. */
    public boolean usesImpinjOctane(String readerId) {
        return readerRepository.findById(readerId)
            .map(r -> r.getBrand() == ReaderBrand.IMPINJ_OCTANE)
            .orElse(false);
    }

    /**
     * Tablas de potencia TX y sensibilidad RX del lector (Impinj conectado) o valores genéricos de referencia.
     */
    public AntennaRfOptions getAntennaRfOptions(String readerId) {
        if (!usesImpinjOctane(readerId)) {
            return AntennaRfOptions.fallback();
        }
        ImpinjReader reader = readers.get(readerId);
        if (reader == null || !reader.isConnected()) {
            return AntennaRfOptions.fallback();
        }
        try {
            FeatureSet f = reader.queryFeatureSet();
            List<Double> tx = new ArrayList<>();
            for (TxPowerTableEntry t : f.getTxPowers()) {
                tx.add(t.Dbm);
            }
            List<Double> rx = new ArrayList<>();
            for (RxSensitivityTableEntry t : f.getRxSensitivities()) {
                rx.add(t.Dbm);
            }
            if (!tx.isEmpty() && !rx.isEmpty()) {
                return new AntennaRfOptions(List.copyOf(tx), List.copyOf(rx), true);
            }
        } catch (OctaneSdkException e) {
            log.warn("getAntennaRfOptions {}: {}", readerId, e.getMessage());
        }
        return AntennaRfOptions.fallback();
    }

    /**
     * Capacidades del hardware vía {@link ImpinjReader#queryFeatureSet()} (lector conectado).
     */
    public Optional<ReaderHardwareInfo> queryHardwareCapabilities(String readerId) {
        Optional<Reader> cfgOpt = readerRepository.findById(readerId);
        if (cfgOpt.isEmpty() || cfgOpt.get().getBrand() != ReaderBrand.IMPINJ_OCTANE) {
            return Optional.empty();
        }
        ImpinjReader reader = readers.get(readerId);
        if (reader == null || !reader.isConnected()) {
            return Optional.empty();
        }
        try {
            FeatureSet f = reader.queryFeatureSet();
            Reader cfg = cfgOpt.get();
            return Optional.of(ReaderHardwareInfo.builder()
                .readerId(readerId)
                .brand(cfg.getBrand().name())
                .antennaCount(Math.max(1, (int) f.getAntennaCount()))
                .modelName(f.getModelName())
                .modelNumber(String.valueOf(f.getModelNumber()))
                .firmwareVersion(f.getFirmwareVersion())
                .xArray(reader.isXArray())
                .build());
        } catch (OctaneSdkException e) {
            log.warn("queryFeatureSet {}: {}", readerId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Crea o actualiza filas {@link Antenna} para los puertos 1..N según el lector Impinj,
     * y deshabilita puertos en BD mayores que N. Reaplica configuración al lector.
     *
     * @return número de puertos sincronizados (1..N)
     */
    public int discoverAndSyncAntennas(String readerId) throws Exception {
        Reader cfg = readerRepository.findById(readerId)
            .orElseThrow(() -> new IllegalArgumentException("Lector no encontrado"));
        if (cfg.getBrand() != ReaderBrand.IMPINJ_OCTANE) {
            throw new IllegalStateException("Detección de antenas solo está implementada para marca Impinj (Octane).");
        }
        ImpinjReader reader = readers.get(readerId);
        if (reader == null || !reader.isConnected()) {
            throw new IllegalStateException("El lector debe estar conectado. Use «Conectar» y espere estado Conectado.");
        }
        FeatureSet features = reader.queryFeatureSet();
        int n = Math.max(1, (int) features.getAntennaCount());
        for (int p = 1; p <= n; p++) {
            short port = (short) p;
            String aid = readerId + "-antenna-" + port;
            Optional<Antenna> existing = antennaRepository.findByReaderIdAndPortNumber(readerId, port);
            Antenna a;
            if (existing.isPresent()) {
                a = existing.get();
                a.setEnabled(true);
            } else {
                a = new Antenna();
                a.setId(aid);
                a.setReaderId(readerId);
                a.setName("Puerto " + port);
                a.setPortNumber(port);
                a.setEnabled(true);
            }
            antennaRepository.save(a);
        }
        for (Antenna ant : antennaRepository.findByReaderId(readerId)) {
            if (ant.getPortNumber() != null && ant.getPortNumber() > n) {
                ant.setEnabled(false);
                antennaRepository.save(ant);
            }
        }
        resetAntennas(readerId);
        return n;
    }

    /**
     * Slot de inventario: una antena, tiempo fijo, sin auto-stop por timeout de túnel.
     */
    public void runInventoryAntennaSlot(String readerId, short antennaPort, long dwellTimeMs) throws Exception {
        if (!usesImpinjOctane(readerId)) {
            log.warn("runInventoryAntennaSlot: lector {} no usa Impinj Octane, se omite", readerId);
            return;
        }
        ImpinjReader reader = readers.get(readerId);
        if (reader == null || !reader.isConnected()) {
            log.warn("runInventoryAntennaSlot: lector {} no conectado", readerId);
            return;
        }
        cancelReadingAutoStop(readerId);
        try {
            reader.stop();
            Settings settings = reader.queryDefaultSettings();
            configureReaderSettingsForPorts(readerId, settings, new short[]{antennaPort});
            reader.applySettings(settings);
            Thread.sleep(200);
            reader.start();
            updateReaderStatus(readerId, true, true);
            Thread.sleep(Math.max(50L, dwellTimeMs));
            reader.stop();
            updateReaderStatus(readerId, true, false);
            Settings restore = reader.queryDefaultSettings();
            configureReaderSettings(readerId, restore);
            reader.applySettings(restore);
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            try {
                reader.stop();
            } catch (Exception ignored) {
            }
            try {
                Settings restore = reader.queryDefaultSettings();
                configureReaderSettings(readerId, restore);
                reader.applySettings(restore);
            } catch (Exception ignored) {
            }
            throw e;
        } catch (Exception e) {
            log.error("runInventoryAntennaSlot {} ant {}: {}", readerId, antennaPort, e.getMessage());
            try {
                reader.stop();
            } catch (Exception ignored) {
            }
            try {
                Settings restore = reader.queryDefaultSettings();
                configureReaderSettings(readerId, restore);
                reader.applySettings(restore);
            } catch (Exception ignored) {
            }
        }
    }

    @PostConstruct
    public void initialize() {
        log.info("Inicializando ReaderManager...");
        try {
            List<Reader> readersList = readerRepository.findByEnabledTrue();
            for (Reader config : readersList) {
                try {
                    connectReader(config);
                } catch (Exception e) {
                    log.error("Error al conectar lector {}: {}", config.getId(), e.getMessage());
                }
            }
            log.info("ReaderManager inicializado con {} lectores", readersList.size());
        } catch (Exception e) {
            log.error("Error en inicialización de ReaderManager: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Cerrando ReaderManager...");
        reconnectExecutor.shutdown();
        try {
            if (!reconnectExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                reconnectExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            reconnectExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        for (Map.Entry<String, ImpinjReader> entry : readers.entrySet()) {
            try {
                ImpinjReader r = entry.getValue();
                if (r.isConnected()) {
                    r.stop();
                    r.disconnect();
                }
            } catch (Exception e) {
                log.warn("Error al desconectar lector {}: {}", entry.getKey(), e.getMessage());
            }
        }
        readers.clear();
    }

    public void connectReader(Reader config) {
        String readerId = config.getId();
        try {
            if (config.getBrand() == null) {
                config.setBrand(ReaderBrand.IMPINJ_OCTANE);
            }
            if (config.getBrand() != ReaderBrand.IMPINJ_OCTANE) {
                disconnectReader(readerId);
                updateReaderStatus(readerId, false, false);
                throw new RuntimeException(
                    "Marca " + config.getBrand() + ": este gateway aún no conecta ese fabricante. Use Impinj (Octane) o espere integración.");
            }
            ImpinjReader reader = new ImpinjReader();
            reader.connect(config.getHostname());

            Settings settings = reader.queryDefaultSettings();
            configureReaderSettings(readerId, settings);

            GatewayTagReportListener tagListener = new GatewayTagReportListener(readerId, tagEventService, readerRepository);
            if (sessionService != null) {
                tagListener.setSessionService(sessionService);
            }
            if (rssiMinDbmConfig != null && !rssiMinDbmConfig.isBlank()) {
                try {
                    tagListener.setRssiMinDbm(Double.parseDouble(rssiMinDbmConfig.trim()));
                } catch (NumberFormatException e) {
                    log.warn("rfid.rssi-min-dbm inválido '{}', se ignora el filtro RSSI", rssiMinDbmConfig);
                }
            }
            reader.setTagReportListener(tagListener);
            reader.setConnectionLostListener(new GatewayConnectionLostListener(readerId, this));

            reader.applySettings(settings);
            Thread.sleep(500);

            readers.put(readerId, reader);
            updateReaderStatus(readerId, true, false);
            webSocketEventService.notifyReaderReconnected(readerId, config.getName());
            log.info("Lector {} conectado (idle, esperando inicio de lectura)", readerId);

        } catch (OctaneSdkException e) {
            log.error("Error SDK al conectar lector {}: {}", readerId, e.getMessage());
            updateReaderStatus(readerId, false, false);
            throw new RuntimeException("Error al conectar lector: " + e.getMessage());
        } catch (RuntimeException e) {
            updateReaderStatus(readerId, false, false);
            throw e;
        } catch (Exception e) {
            log.error("Error al conectar lector {}: {}", readerId, e.getMessage());
            updateReaderStatus(readerId, false, false);
            throw new RuntimeException("Error al conectar lector: " + e.getMessage());
        }
    }

    private void configureReaderSettings(String readerId, Settings settings) throws OctaneSdkException {
        if (!usesImpinjOctane(readerId)) {
            return;
        }
        List<Antenna> enabledAntennas = antennaRepository.findByReaderIdAndEnabledTrue(readerId);
        short[] ports;
        if (enabledAntennas.isEmpty()) {
            ports = new short[]{1};
        } else {
            ports = new short[enabledAntennas.size()];
            for (int i = 0; i < enabledAntennas.size(); i++) {
                ports[i] = enabledAntennas.get(i).getPortNumber();
            }
        }
        configureReaderSettingsForPorts(readerId, settings, ports, enabledAntennas);
    }

    private void configureReaderSettingsForPorts(String readerId, Settings settings, short[] ports) throws OctaneSdkException {
        List<Antenna> enabled = antennaRepository.findByReaderIdAndEnabledTrue(readerId);
        configureReaderSettingsForPorts(readerId, settings, ports, enabled);
    }

    private void configureReaderSettingsForPorts(
        String readerId,
        Settings settings,
        short[] ports,
        List<Antenna> enabledAntennas
    ) throws OctaneSdkException {
        settings.setReaderMode(ReaderMode.AutoSetDenseReader);
        settings.setSearchMode(SearchMode.SingleTarget);
        settings.setSession((short) 1);

        ReportConfig report = settings.getReport();
        report.setMode(ReportMode.Individual);
        report.setIncludeAntennaPortNumber(true);
        report.setIncludePeakRssi(true);
        report.setIncludeLastSeenTime(true);
        report.setIncludeSeenCount(true);

        AntennaConfigGroup antennas = settings.getAntennas();
        antennas.disableAll();

        if (ports == null || ports.length == 0) {
            ports = new short[]{1};
        }
        Map<Short, Antenna> byPort = new HashMap<>();
        if (enabledAntennas != null) {
            for (Antenna a : enabledAntennas) {
                if (a.getPortNumber() != null) {
                    byPort.put(a.getPortNumber(), a);
                }
            }
        }
        antennas.enableById(ports);
        for (short port : ports) {
            AntennaConfig ac = antennas.getAntenna(port);
            if (ac == null) {
                continue;
            }
            Antenna db = byPort.get(port);
            if (db != null && db.getTxPowerDbm() != null) {
                ac.setIsMaxTxPower(false);
                ac.setTxPowerinDbm(db.getTxPowerDbm());
            } else {
                ac.setIsMaxTxPower(true);
            }
            if (db != null && db.getRxSensitivityDbm() != null) {
                ac.setIsMaxRxSensitivity(false);
                ac.setRxSensitivityinDbm(db.getRxSensitivityDbm());
            } else {
                ac.setIsMaxRxSensitivity(true);
            }
        }
    }

    public void disconnectReader(String readerId) {
        cancelReadingAutoStop(readerId);
        ImpinjReader reader = readers.remove(readerId);
        if (reader != null) {
            try {
                if (reader.isConnected()) {
                    reader.stop();
                    reader.disconnect();
                }
            } catch (Exception e) {
                log.warn("Error al desconectar {}: {}", readerId, e.getMessage());
            }
        }
        updateReaderStatus(readerId, false, false);
    }

    public void startReader(String readerId) {
        ImpinjReader reader = readers.get(readerId);
        if (reader != null && reader.isConnected()) {
            try {
                cancelReadingAutoStop(readerId);
                reader.start();
                updateReaderStatus(readerId, true, true);
                if (!isContinuousInventoryReader(readerId) && readingTimeoutMinutes > 0) {
                    scheduleReadingAutoStop(readerId);
                }
            } catch (Exception e) {
                log.error("Error al iniciar lectura {}: {}", readerId, e.getMessage());
            }
        }
    }

    public void stopReader(String readerId) {
        ImpinjReader reader = readers.get(readerId);
        if (reader != null && reader.isConnected()) {
            try {
                cancelReadingAutoStop(readerId);
                reader.stop();
                updateReaderStatus(readerId, true, false);
            } catch (Exception e) {
                log.error("Error al detener lectura {}: {}", readerId, e.getMessage());
            }
        }
    }

    private void cancelReadingAutoStop(String readerId) {
        ScheduledFuture<?> future = readingAutoStopFutures.remove(readerId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void scheduleReadingAutoStop(String readerId) {
        if (readingTimeoutMinutes <= 0 || isContinuousInventoryReader(readerId)) {
            return;
        }
        cancelReadingAutoStop(readerId);
        ScheduledFuture<?> future = reconnectExecutor.schedule(() -> {
            readingAutoStopFutures.remove(readerId);
            log.info("Auto-stop: deteniendo lectura del lector {} tras {} min", readerId, readingTimeoutMinutes);
            stopReader(readerId);
        }, readingTimeoutMinutes, TimeUnit.MINUTES);
        readingAutoStopFutures.put(readerId, future);
    }

    public void resetReader(String readerId) {
        Optional<Reader> configOpt = readerRepository.findById(readerId);
        if (configOpt.isEmpty()) return;
        Reader config = configOpt.get();
        disconnectReader(readerId);
        try {
            Thread.sleep(2000);
            connectReader(config);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Reset interrumpido para {}", readerId);
        }
    }

    public void rebootReader(String readerId) {
        Optional<Reader> configOpt = readerRepository.findById(readerId);
        if (configOpt.isEmpty()) return;
        Reader config = configOpt.get();
        disconnectReader(readerId);
        try {
            Thread.sleep(5000);
            connectReader(config);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Reboot interrumpido para {}", readerId);
        }
    }

    public void resetAntennas(String readerId) {
        if (!usesImpinjOctane(readerId)) {
            log.warn("resetAntennas: lector {} no es Impinj Octane", readerId);
            return;
        }
        ImpinjReader reader = readers.get(readerId);
        if (reader == null || !reader.isConnected()) {
            log.warn("Lector {} no conectado, no se puede resetear antenas", readerId);
            return;
        }
        try {
            cancelReadingAutoStop(readerId);
            reader.stop();
            Settings settings = reader.queryDefaultSettings();
            configureReaderSettings(readerId, settings);
            reader.applySettings(settings);
            Thread.sleep(500);
            updateReaderStatus(readerId, true, false);
            log.info("Antenas del lector {} reseteadas (idle)", readerId);
        } catch (Exception e) {
            log.error("Error al resetear antenas del lector {}: {}", readerId, e.getMessage());
        }
    }

    public void handleConnectionLost(String readerId) {
        readers.remove(readerId);
        updateReaderStatus(readerId, false, false);
        webSocketEventService.notifyReaderDisconnected(readerId,
            readerRepository.findById(readerId).map(Reader::getName).orElse(readerId));
        scheduleReconnect(readerId);
    }

    public void scheduleReconnect(String readerId) {
        Optional<Reader> configOpt = readerRepository.findById(readerId);
        if (configOpt.isEmpty() || !Boolean.TRUE.equals(configOpt.get().getEnabled())) {
            return;
        }
        Reader config = configOpt.get();
        log.info("Programando reconexión del lector {} en 30 segundos", readerId);
        reconnectExecutor.schedule(() -> {
            try {
                connectReader(config);
            } catch (Exception e) {
                log.warn("Reconexión fallida para {}, reintentando más tarde: {}", readerId, e.getMessage());
                scheduleReconnect(readerId);
            }
        }, 30, TimeUnit.SECONDS);
    }

    private void updateReaderStatus(String readerId, boolean connected, boolean reading) {
        readerRepository.findById(readerId).ifPresent(r -> {
            r.setIsConnected(connected);
            r.setIsReading(reading);
            if (connected) r.setLastSeen(java.time.LocalDateTime.now());
            readerRepository.save(r);
        });
    }
}
