package com.rfidgateway.reader;

import com.impinj.octane.*;
import com.rfidgateway.model.Antenna;
import com.rfidgateway.model.Reader;
import com.rfidgateway.repository.AntennaRepository;
import com.rfidgateway.repository.ReaderRepository;
import com.rfidgateway.tag.TagEventService;
import com.rfidgateway.tag.WebSocketEventService;
import com.rfidgateway.session.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Component
public class ReaderManager {

    private final Map<String, ImpinjReader> readers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reconnectExecutor = Executors.newScheduledThreadPool(5);

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
            ImpinjReader reader = new ImpinjReader();
            reader.connect(config.getHostname());

            Settings settings = reader.queryDefaultSettings();
            configureReaderSettings(readerId, settings);

            GatewayTagReportListener tagListener = new GatewayTagReportListener(readerId, tagEventService);
            if (sessionService != null) {
                tagListener.setSessionService(sessionService);
            }
            reader.setTagReportListener(tagListener);
            reader.setConnectionLostListener(new GatewayConnectionLostListener(readerId, this));

            reader.applySettings(settings);
            Thread.sleep(500);
            reader.start();

            readers.put(readerId, reader);
            updateReaderStatus(readerId, true, true);
            webSocketEventService.notifyReaderReconnected(readerId, config.getName());
            log.info("Lector {} conectado y leyendo", readerId);

        } catch (OctaneSdkException e) {
            log.error("Error SDK al conectar lector {}: {}", readerId, e.getMessage());
            updateReaderStatus(readerId, false, false);
            throw new RuntimeException("Error al conectar lector: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error al conectar lector {}: {}", readerId, e.getMessage());
            updateReaderStatus(readerId, false, false);
            throw new RuntimeException("Error al conectar lector: " + e.getMessage());
        }
    }

    private void configureReaderSettings(String readerId, Settings settings) throws OctaneSdkException {
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

        List<Antenna> enabledAntennas = antennaRepository.findByReaderIdAndEnabledTrue(readerId);
        if (enabledAntennas.isEmpty()) {
            short[] ports = {1};
            antennas.enableById(ports);
            AntennaConfig ac = antennas.getAntenna((short) 1);
            ac.setIsMaxTxPower(true);
            ac.setIsMaxRxSensitivity(true);
        } else {
            short[] ports = new short[enabledAntennas.size()];
            for (int i = 0; i < enabledAntennas.size(); i++) {
                ports[i] = enabledAntennas.get(i).getPortNumber();
                AntennaConfig ac = antennas.getAntenna(ports[i]);
                if (ac != null) {
                    ac.setIsMaxTxPower(true);
                    ac.setIsMaxRxSensitivity(true);
                }
            }
            antennas.enableById(ports);
        }
    }

    public void disconnectReader(String readerId) {
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
                reader.start();
                updateReaderStatus(readerId, true, true);
            } catch (Exception e) {
                log.error("Error al iniciar lectura {}: {}", readerId, e.getMessage());
            }
        }
    }

    public void stopReader(String readerId) {
        ImpinjReader reader = readers.get(readerId);
        if (reader != null && reader.isConnected()) {
            try {
                reader.stop();
                updateReaderStatus(readerId, true, false);
            } catch (Exception e) {
                log.error("Error al detener lectura {}: {}", readerId, e.getMessage());
            }
        }
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
        ImpinjReader reader = readers.get(readerId);
        if (reader == null || !reader.isConnected()) {
            log.warn("Lector {} no conectado, no se puede resetear antenas", readerId);
            return;
        }
        try {
            reader.stop();
            Settings settings = reader.queryDefaultSettings();
            configureReaderSettings(readerId, settings);
            reader.applySettings(settings);
            Thread.sleep(500);
            reader.start();
            updateReaderStatus(readerId, true, true);
            log.info("Antenas del lector {} reseteadas correctamente", readerId);
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
