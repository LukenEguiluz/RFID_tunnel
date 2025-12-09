package com.rfidgateway.reader;

import com.impinj.octane.*;
import com.rfidgateway.model.Reader;
import com.rfidgateway.model.Antenna;
import com.rfidgateway.repository.ReaderRepository;
import com.rfidgateway.repository.AntennaRepository;
import com.rfidgateway.tag.TagEventService;
import com.rfidgateway.tag.WebSocketEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ReaderManager {

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private AntennaRepository antennaRepository;

    @Autowired
    private TagEventService tagEventService;
    
    @Autowired
    private WebSocketEventService webSocketEventService;

    private final Map<String, ImpinjReader> readers = new ConcurrentHashMap<>();
    private final Map<String, ReaderInfo> readerInfos = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reconnectExecutor = Executors.newScheduledThreadPool(5);

    @PostConstruct
    public void initialize() {
        log.info("Inicializando ReaderManager...");
        List<Reader> enabledReaders = readerRepository.findByEnabledTrue();
        
        for (Reader readerConfig : enabledReaders) {
            connectReader(readerConfig);
        }
        
        log.info("ReaderManager inicializado con {} lectores", readers.size());
    }

    @PreDestroy
    public void shutdown() {
        log.info("Cerrando ReaderManager...");
        reconnectExecutor.shutdown();
        
        for (Map.Entry<String, ImpinjReader> entry : readers.entrySet()) {
            try {
                ImpinjReader reader = entry.getValue();
                if (reader.isConnected()) {
                    reader.stop();
                    reader.disconnect();
                }
            } catch (Exception e) {
                log.error("Error al desconectar lector {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        readers.clear();
        readerInfos.clear();
    }

    public void connectReader(Reader readerConfig) {
        String readerId = readerConfig.getId();
        
        if (readers.containsKey(readerId)) {
            log.warn("Lector {} ya está conectado", readerId);
            return;
        }

        ImpinjReader impinjReader = new ImpinjReader();
        impinjReader.setName(readerConfig.getName());
        
        ReaderInfo info = new ReaderInfo(readerConfig, impinjReader);
        readerInfos.put(readerId, info);

        try {
            log.info("Conectando a lector {} ({}) en {}", 
                    readerConfig.getName(), readerId, readerConfig.getHostname());
            
            impinjReader.connect(readerConfig.getHostname());
            
            // Reset: Detener cualquier operación en curso antes de configurar
            try {
                if (impinjReader.isConnected()) {
                    log.info("Deteniendo cualquier operación en curso en lector {}...", readerId);
                    impinjReader.stop();
                    try {
                        Thread.sleep(500); // Pequeña pausa para asegurar que se detuvo
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Interrupción durante pausa de reset");
                    }
                }
            } catch (Exception e) {
                log.warn("No se pudo detener operación previa (puede ser normal): {}", e.getMessage());
            }
            
            // Configurar settings PRIMERO (igual que el ejemplo que funciona)
            Settings settings = impinjReader.queryDefaultSettings();
            configureReaderSettings(settings, readerId);
            
            // Configurar listeners DESPUÉS de configurar settings pero ANTES de applySettings
            // (Este es el orden correcto según el ejemplo que funciona)
            log.info("Configurando listeners para lector {}...", readerId);
            impinjReader.setTagReportListener(new GatewayTagReportListener(readerId, tagEventService));
            impinjReader.setConnectionLostListener(new GatewayConnectionLostListener(readerId, this));
            log.info("Listeners configurados correctamente");
            
            // Aplicar configuración
            log.info("Aplicando configuración al lector {}...", readerId);
            impinjReader.applySettings(settings);
            
            // Verificar que el listener esté configurado
            log.info("Listener de tags configurado para lector {}", readerId);
            
            // Pequeña pausa antes de iniciar
            try {
                Thread.sleep(1000); // Aumentar a 1 segundo para asegurar que la configuración se aplique
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Interrupción durante pausa antes de iniciar");
            }
            
            // Verificar estado antes de iniciar
            Status status = impinjReader.queryStatus();
            log.info("Estado del lector {} antes de iniciar: Singulating={}, Connected={}", 
                    readerId, status.getIsSingulating(), status.getIsConnected());
            
            // Iniciar lectura continua
            log.info("Iniciando inventario en lector {}...", readerId);
            impinjReader.start();
            
            // Verificar estado después de iniciar
            try {
                Thread.sleep(500);
                Status statusAfter = impinjReader.queryStatus();
                log.info("Estado del lector {} después de iniciar: Singulating={}, Connected={}", 
                        readerId, statusAfter.getIsSingulating(), statusAfter.getIsConnected());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
            readers.put(readerId, impinjReader);
            
            // Actualizar estado en BD
            readerConfig.setIsConnected(true);
            readerConfig.setIsReading(true);
            readerConfig.setLastSeen(java.time.LocalDateTime.now());
            readerRepository.save(readerConfig);
            
            log.info("Lector {} conectado y leyendo exitosamente", readerConfig.getName());
            
            // Notificar reconexión vía WebSocket
            webSocketEventService.notifyReaderReconnected(readerId, readerConfig.getName());
            
        } catch (OctaneSdkException e) {
            log.error("Error al conectar lector {}: {}", readerConfig.getName(), e.getMessage());
            readerConfig.setIsConnected(false);
            readerConfig.setIsReading(false);
            readerRepository.save(readerConfig);
            
            // Intentar reconexión automática
            scheduleReconnect(readerId, readerConfig);
        }
    }

    private void configureReaderSettings(Settings settings, String readerId) {
        try {
            // Configuración para inventario continuo
            settings.setReaderMode(ReaderMode.AutoSetDenseReader);
            settings.setSearchMode(SearchMode.SingleTarget);
            settings.setSession((short) 1);
            
            // Configurar reporte (igual que el ejemplo que funciona)
            ReportConfig report = settings.getReport();
            report.setMode(ReportMode.Individual);
            report.setIncludeAntennaPortNumber(true);
            report.setIncludePeakRssi(true);
            report.setIncludeLastSeenTime(true);
            report.setIncludeSeenCount(true);
            log.info("Configuración de reporte: Mode={}, IncludeAntenna={}, IncludeRSSI={}", 
                    report.getMode(), report.getIncludeAntennaPortNumber(), report.getIncludePeakRssi());
            
            // Configurar antenas habilitadas
            AntennaConfigGroup antennas = settings.getAntennas();
            antennas.disableAll();
            
            List<Antenna> enabledAntennas = antennaRepository.findByReaderIdAndEnabledTrue(readerId);
            if (enabledAntennas.isEmpty()) {
                // Si no hay configuración, habilitar solo antena 1 por defecto (igual que el ejemplo)
                log.warn("No hay antenas configuradas para lector {}. Habilitando antena 1 por defecto.", readerId);
                antennas.enableById(new short[]{1});
                // Configurar potencia máxima por defecto
                try {
                    AntennaConfig antennaConfig = antennas.getAntenna((short) 1);
                    antennaConfig.setIsMaxTxPower(true);
                    antennaConfig.setIsMaxRxSensitivity(true);
                    log.info("Antena 1 configurada con potencia y sensibilidad máximas");
                } catch (OctaneSdkException e) {
                    log.warn("No se pudo configurar antena 1: {}", e.getMessage());
                }
            } else {
                List<Short> antennaPorts = new ArrayList<>();
                log.info("Configurando {} antena(s) para lector {}", enabledAntennas.size(), readerId);
                for (Antenna antenna : enabledAntennas) {
                    short port = antenna.getPortNumber().shortValue();
                    antennaPorts.add(port);
                    
                    try {
                        AntennaConfig antennaConfig = antennas.getAntenna(port);
                        if (antenna.getTxPowerDbm() != null) {
                            antennaConfig.setIsMaxTxPower(false);
                            antennaConfig.setTxPowerinDbm(antenna.getTxPowerDbm());
                            log.info("Antena {}: TxPower = {} dBm", port, antenna.getTxPowerDbm());
                        } else {
                            antennaConfig.setIsMaxTxPower(true);
                            log.info("Antena {}: TxPower = MAX", port);
                        }
                        if (antenna.getRxSensitivityDbm() != null) {
                            antennaConfig.setIsMaxRxSensitivity(false);
                            antennaConfig.setRxSensitivityinDbm(antenna.getRxSensitivityDbm());
                            log.info("Antena {}: RxSensitivity = {} dBm", port, antenna.getRxSensitivityDbm());
                        } else {
                            antennaConfig.setIsMaxRxSensitivity(true);
                            log.info("Antena {}: RxSensitivity = MAX", port);
                        }
                    } catch (OctaneSdkException e) {
                        log.error("Error al configurar antena {}: {}", port, e.getMessage());
                    }
                }
                
                short[] portsArray = new short[antennaPorts.size()];
                for (int i = 0; i < antennaPorts.size(); i++) {
                    portsArray[i] = antennaPorts.get(i);
                }
                antennas.enableById(portsArray);
                log.info("Antenas habilitadas: {}", java.util.Arrays.toString(portsArray));
            }
        } catch (OctaneSdkException e) {
            log.error("Error al configurar settings del lector {}: {}", readerId, e.getMessage());
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
                log.error("Error al desconectar lector {}: {}", readerId, e.getMessage());
            }
        }
        
        readerInfos.remove(readerId);
        
        Reader readerConfig = readerRepository.findById(readerId).orElse(null);
        if (readerConfig != null) {
            readerConfig.setIsConnected(false);
            readerConfig.setIsReading(false);
            readerRepository.save(readerConfig);
        }
    }

    public void startReader(String readerId) {
        ImpinjReader reader = readers.get(readerId);
        if (reader != null && reader.isConnected()) {
            try {
                reader.start();
                Reader readerConfig = readerRepository.findById(readerId).orElse(null);
                if (readerConfig != null) {
                    readerConfig.setIsReading(true);
                    readerRepository.save(readerConfig);
                }
                log.info("Lector {} iniciado", readerId);
            } catch (OctaneSdkException e) {
                log.error("Error al iniciar lector {}: {}", readerId, e.getMessage());
            }
        }
    }

    public void stopReader(String readerId) {
        ImpinjReader reader = readers.get(readerId);
        if (reader != null && reader.isConnected()) {
            try {
                reader.stop();
                Reader readerConfig = readerRepository.findById(readerId).orElse(null);
                if (readerConfig != null) {
                    readerConfig.setIsReading(false);
                    readerRepository.save(readerConfig);
                }
                log.info("Lector {} detenido", readerId);
            } catch (OctaneSdkException e) {
                log.error("Error al detener lector {}: {}", readerId, e.getMessage());
            }
        }
    }

    private void scheduleReconnect(String readerId, Reader readerConfig) {
        reconnectExecutor.schedule(() -> {
            log.info("Intentando reconectar lector {}...", readerConfig.getName());
            if (!readers.containsKey(readerId)) {
                connectReader(readerConfig);
            }
        }, 30, TimeUnit.SECONDS);
    }

    public void handleConnectionLost(String readerId) {
        log.warn("Conexión perdida con lector {}", readerId);
        
        Reader readerConfig = readerRepository.findById(readerId).orElse(null);
        if (readerConfig != null) {
            readerConfig.setIsConnected(false);
            readerConfig.setIsReading(false);
            readerRepository.save(readerConfig);
            
            // Notificar vía WebSocket
            webSocketEventService.notifyReaderDisconnected(readerId, readerConfig.getName());
            
            // Intentar reconexión
            if (readerConfig.getEnabled()) {
                readers.remove(readerId);
                scheduleReconnect(readerId, readerConfig);
            }
        }
    }

    public Map<String, ImpinjReader> getReaders() {
        return Collections.unmodifiableMap(readers);
    }

    public ImpinjReader getReader(String readerId) {
        return readers.get(readerId);
    }

    public ReaderInfo getReaderInfo(String readerId) {
        return readerInfos.get(readerId);
    }

    /**
     * Reinicia la conexión del lector (desconecta y reconecta)
     */
    public void resetReader(String readerId) {
        log.info("Reiniciando conexión del lector {}...", readerId);
        
        Reader readerConfig = readerRepository.findById(readerId).orElse(null);
        if (readerConfig == null) {
            log.error("Lector {} no encontrado", readerId);
            return;
        }
        
        // Desconectar completamente
        disconnectReader(readerId);
        
        // Esperar un poco más para asegurar desconexión completa
        try {
            Thread.sleep(2000); // 2 segundos
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupción durante espera de reset");
        }
        
        // Reconectar
        if (readerConfig.getEnabled()) {
            log.info("Reconectando lector {} después de reset...", readerId);
            connectReader(readerConfig);
        }
    }

    /**
     * Reboot completo del lector: desconecta, espera más tiempo y reconecta
     */
    public void rebootReader(String readerId) {
        log.info("Ejecutando reboot completo del lector {}...", readerId);
        
        Reader readerConfig = readerRepository.findById(readerId).orElse(null);
        if (readerConfig == null) {
            log.error("Lector {} no encontrado", readerId);
            return;
        }
        
        // Desconectar completamente
        disconnectReader(readerId);
        
        // Esperar más tiempo para un reboot completo
        try {
            log.info("Esperando 5 segundos antes de reconectar...");
            Thread.sleep(5000); // 5 segundos
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupción durante espera de reboot");
        }
        
        // Reconectar
        if (readerConfig.getEnabled()) {
            log.info("Reconectando lector {} después de reboot...", readerId);
            connectReader(readerConfig);
        }
    }

    /**
     * Reinicia solo la configuración de antenas del lector
     */
    public void resetAntennas(String readerId) {
        log.info("Reiniciando configuración de antenas del lector {}...", readerId);
        
        ImpinjReader reader = readers.get(readerId);
        if (reader == null || !reader.isConnected()) {
            log.warn("Lector {} no está conectado. No se puede resetear antenas.", readerId);
            return;
        }
        
        try {
            // Detener lectura
            reader.stop();
            
            // Esperar un momento
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Obtener configuración actual
            Settings settings = reader.queryDefaultSettings();
            
            // Reconfigurar antenas
            configureReaderSettings(settings, readerId);
            
            // Aplicar nueva configuración
            log.info("Aplicando nueva configuración de antenas...");
            reader.applySettings(settings);
            
            // Esperar un momento antes de reiniciar
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Reiniciar lectura
            reader.start();
            
            log.info("Configuración de antenas reiniciada para lector {}", readerId);
            
        } catch (OctaneSdkException e) {
            log.error("Error al resetear antenas del lector {}: {}", readerId, e.getMessage());
        }
    }

    public static class ReaderInfo {
        private final Reader config;
        private final ImpinjReader impinjReader;

        public ReaderInfo(Reader config, ImpinjReader impinjReader) {
            this.config = config;
            this.impinjReader = impinjReader;
        }

        public Reader getConfig() {
            return config;
        }

        public ImpinjReader getImpinjReader() {
            return impinjReader;
        }
    }
}

