package com.rfidgateway.reader;

import com.impinj.octane.*;
import com.rfidgateway.model.Antenna;
import com.rfidgateway.model.Reader;
import com.rfidgateway.repository.AntennaRepository;
import com.rfidgateway.repository.ReaderRepository;
import com.rfidgateway.tag.TagEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de lectores RFID
 */
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
    private com.rfidgateway.session.SessionService sessionService;
    
    // Almacenar instancias de ImpinjReader por readerId
    private final Map<String, ImpinjReader> readers = new ConcurrentHashMap<>();
    
    /**
     * Inicia lectura de sesión en un lector
     * @param readerId ID del lector
     * @param sessionId ID de la sesión
     */
    public void startSessionReading(String readerId, String sessionId) {
        log.info("Iniciando lectura de sesión {} en lector {}", sessionId, readerId);
        
        try {
            // Obtener configuración del lector desde BD
            Reader readerConfig = readerRepository.findById(readerId).orElse(null);
            if (readerConfig == null) {
                log.error("Lector {} no encontrado en la base de datos", readerId);
                return;
            }
            
            // Verificar que el lector esté conectado
            if (readerConfig.getIsConnected() == null || !readerConfig.getIsConnected()) {
                log.warn("Lector {} no está conectado. Intentando conectar...", readerId);
                connectReader(readerConfig);
            }
            
            // Obtener o crear instancia de ImpinjReader
            ImpinjReader reader = readers.get(readerId);
            if (reader == null || !reader.isConnected()) {
                log.warn("Lector {} no tiene instancia conectada. Reconectando...", readerId);
                connectReader(readerConfig);
                reader = readers.get(readerId);
            }
            
            if (reader == null || !reader.isConnected()) {
                log.error("No se pudo conectar al lector {} para iniciar sesión {}", readerId, sessionId);
                return;
            }
            
            // Obtener configuración por defecto
            Settings settings = reader.queryDefaultSettings();
            
            // Configurar modo de lectura
            settings.setReaderMode(ReaderMode.AutoSetDenseReader);
            settings.setSearchMode(SearchMode.SingleTarget);
            settings.setSession((short) 1);
            
            // Configurar reporte
            ReportConfig report = settings.getReport();
            report.setMode(ReportMode.Individual);
            report.setIncludeAntennaPortNumber(true);
            report.setIncludePeakRssi(true);
            report.setIncludeLastSeenTime(true);
            report.setIncludeSeenCount(true);
            
            // Obtener antenas habilitadas desde BD
            List<Antenna> antennas = antennaRepository.findByReaderIdAndEnabledTrue(readerId);
            
            if (antennas.isEmpty()) {
                log.warn("No hay antenas habilitadas para el lector {}", readerId);
                return;
            }
            
            // Configurar antenas
            AntennaConfigGroup antennaConfigGroup = settings.getAntennas();
            antennaConfigGroup.disableAll();
            
            for (Antenna antenna : antennas) {
                short portNumber = antenna.getPortNumber();
                AntennaConfig antennaConfig = antennaConfigGroup.getAntenna(portNumber);
                
                if (antennaConfig != null) {
                    antennaConfig.setEnabled(true);
                    
                    // Configurar potencia
                    if (readerConfig.getUseDefaultPower() != null && readerConfig.getUseDefaultPower() 
                        && readerConfig.getDefaultTxPowerDbm() != null) {
                        // Usar potencia por defecto del lector
                        antennaConfig.setIsMaxTxPower(false);
                        antennaConfig.setTxPowerinDbm(readerConfig.getDefaultTxPowerDbm());
                    } else if (antenna.getTxPowerDbm() != null) {
                        // Usar potencia individual de la antena
                        antennaConfig.setIsMaxTxPower(false);
                        
                        // Verificar límite máximo si está configurado
                        if (readerConfig.getMaxTxPowerDbm() != null) {
                            double power = Math.min(antenna.getTxPowerDbm(), readerConfig.getMaxTxPowerDbm());
                            antennaConfig.setTxPowerinDbm(power);
                        } else {
                            antennaConfig.setTxPowerinDbm(antenna.getTxPowerDbm());
                        }
                    } else {
                        // Usar potencia máxima
                        antennaConfig.setIsMaxTxPower(true);
                    }
                    
                    // Configurar sensibilidad
                    if (readerConfig.getDefaultRxSensitivityDbm() != null) {
                        // Usar sensibilidad por defecto del lector
                        antennaConfig.setIsMaxRxSensitivity(false);
                        antennaConfig.setRxSensitivityinDbm(readerConfig.getDefaultRxSensitivityDbm());
                    } else if (antenna.getRxSensitivityDbm() != null) {
                        // Usar sensibilidad individual de la antena
                        antennaConfig.setIsMaxRxSensitivity(false);
                        antennaConfig.setRxSensitivityinDbm(antenna.getRxSensitivityDbm());
                    } else {
                        // Usar sensibilidad máxima
                        antennaConfig.setIsMaxRxSensitivity(true);
                    }
                    
                    log.debug("Antena {} configurada para lector {} - Puerto: {}, Potencia: {}, Sensibilidad: {}", 
                        antenna.getId(), readerId, portNumber,
                        antennaConfig.getIsMaxTxPower() ? "MAX" : antennaConfig.getTxPowerinDbm() + " dBm",
                        antennaConfig.getIsMaxRxSensitivity() ? "MAX" : antennaConfig.getRxSensitivityinDbm() + " dBm");
                }
            }
            
            // Configurar listener si no está configurado
            TagReportListener currentListener = reader.getTagReportListener();
            if (currentListener == null || !(currentListener instanceof GatewayTagReportListener)) {
                GatewayTagReportListener listener = new GatewayTagReportListener(readerId, tagEventService);
                listener.setSessionService(sessionService);
                reader.setTagReportListener(listener);
                
                // Configurar connection lost listener
                GatewayConnectionLostListener connectionLostListener = 
                    new GatewayConnectionLostListener(readerId, this);
                reader.setConnectionLostListener(connectionLostListener);
            }
            
            // Aplicar configuración
            reader.applySettings(settings);
            log.info("Configuración aplicada para lector {}", readerId);
            
            // Iniciar lectura
            reader.start();
            log.info("Lectura iniciada para lector {} en sesión {}", readerId, sessionId);
            
            // Actualizar estado en BD
            readerConfig.setIsReading(true);
            readerRepository.save(readerConfig);
            
        } catch (OctaneSdkException e) {
            log.error("Error del SDK al iniciar lectura para lector {}: {}", readerId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al iniciar lectura para lector {}: {}", readerId, e.getMessage(), e);
        }
    }
    
    /**
     * Detiene lectura de sesión en un lector
     * @param readerId ID del lector
     */
    public void stopSessionReading(String readerId) {
        log.info("Deteniendo lectura de sesión en lector {}", readerId);
        
        try {
            ImpinjReader reader = readers.get(readerId);
            if (reader != null && reader.isConnected()) {
                reader.stop();
                log.info("Lectura detenida para lector {}", readerId);
                
                // Actualizar estado en BD
                Reader readerConfig = readerRepository.findById(readerId).orElse(null);
                if (readerConfig != null) {
                    readerConfig.setIsReading(false);
                    readerRepository.save(readerConfig);
                }
            } else {
                log.warn("Lector {} no está conectado, no se puede detener lectura", readerId);
            }
        } catch (OctaneSdkException e) {
            log.error("Error del SDK al detener lectura para lector {}: {}", readerId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al detener lectura para lector {}: {}", readerId, e.getMessage(), e);
        }
    }
    
    /**
     * Reinicia configuración de antenas
     * @param readerId ID del lector
     */
    public void resetAntennas(String readerId) {
        log.info("Reiniciando configuración de antenas en lector {}", readerId);
        // Por ahora, simplemente reconectamos el lector
        Reader readerConfig = readerRepository.findById(readerId).orElse(null);
        if (readerConfig != null) {
            connectReader(readerConfig);
        }
    }
    
    /**
     * Inicia lectura en un lector
     * @param readerId ID del lector
     */
    public void startReader(String readerId) {
        log.info("Iniciando lectura en lector {}", readerId);
        
        Reader readerConfig = readerRepository.findById(readerId).orElse(null);
        if (readerConfig != null) {
            connectReader(readerConfig);
        } else {
            log.error("Lector {} no encontrado", readerId);
        }
    }
    
    /**
     * Detiene lectura en un lector
     * @param readerId ID del lector
     */
    public void stopReader(String readerId) {
        log.info("Deteniendo lectura en lector {}", readerId);
        stopSessionReading(readerId);
    }
    
    /**
     * Reinicia un lector (desconecta y reconecta)
     * @param readerId ID del lector
     */
    public void resetReader(String readerId) {
        log.info("Reiniciando lector {}", readerId);
        
        disconnectReader(readerId);
        
        try {
            Thread.sleep(1000); // Esperar 1 segundo antes de reconectar
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Reader readerConfig = readerRepository.findById(readerId).orElse(null);
        if (readerConfig != null) {
            connectReader(readerConfig);
        }
    }
    
    /**
     * Reinicia completamente un lector (reboot)
     * @param readerId ID del lector
     */
    public void rebootReader(String readerId) {
        log.info("Iniciando reboot completo del lector {}", readerId);
        
        try {
            // Obtener configuración del lector
            Reader readerConfig = readerRepository.findById(readerId).orElse(null);
            if (readerConfig == null) {
                log.error("Lector {} no encontrado para reboot", readerId);
                return;
            }
            
            // Detener lectura si está activa
            ImpinjReader reader = readers.get(readerId);
            if (reader != null && reader.isConnected()) {
                try {
                    // Detener lectura primero
                    if (readerConfig.getIsReading() != null && readerConfig.getIsReading()) {
                        reader.stop();
                        log.info("Lectura detenida antes del reboot para lector {}", readerId);
                    }
                    
                    // Intentar hacer reboot del lector físico usando el SDK
                    // Nota: El SDK de Octane no tiene un método directo de reboot,
                    // así que desconectamos y esperamos más tiempo para que el lector se reinicie
                    log.info("Desconectando lector {} para reboot", readerId);
                    reader.disconnect();
                    
                } catch (OctaneSdkException e) {
                    log.warn("Error al desconectar lector {} antes del reboot: {}", readerId, e.getMessage());
                }
            }
            
            // Limpiar instancia
            readers.remove(readerId);
            
            // Actualizar estado en BD
            readerConfig.setIsConnected(false);
            readerConfig.setIsReading(false);
            readerRepository.save(readerConfig);
            
            // Esperar 5 segundos para que el lector se reinicie completamente
            log.info("Esperando 5 segundos para que el lector {} se reinicie...", readerId);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Espera interrumpida durante reboot del lector {}", readerId);
            }
            
            // Reconectar el lector
            log.info("Reconectando lector {} después del reboot", readerId);
            connectReader(readerConfig);
            
            log.info("Reboot completado para lector {}", readerId);
            
        } catch (Exception e) {
            log.error("Error durante reboot del lector {}: {}", readerId, e.getMessage(), e);
        }
    }
    
    /**
     * Conecta un lector
     * @param readerConfig Configuración del lector
     */
    public void connectReader(Reader readerConfig) {
        String readerId = readerConfig.getId();
        String hostname = readerConfig.getHostname();
        
        log.info("Conectando lector {} en {}", readerId, hostname);
        
        try {
            // Si ya hay una instancia conectada, desconectarla primero
            ImpinjReader existingReader = readers.get(readerId);
            if (existingReader != null && existingReader.isConnected()) {
                try {
                    existingReader.disconnect();
                } catch (Exception e) {
                    log.warn("Error al desconectar lector existente {}: {}", readerId, e.getMessage());
                }
            }
            
            // Crear nueva instancia
            ImpinjReader reader = new ImpinjReader();
            
            // Conectar
            reader.connect(hostname);
            log.info("Conexión establecida con lector {} en {}", readerId, hostname);
            
            // Almacenar instancia
            readers.put(readerId, reader);
            
            // Configurar listeners
            GatewayTagReportListener tagListener = new GatewayTagReportListener(readerId, tagEventService);
            tagListener.setSessionService(sessionService);
            reader.setTagReportListener(tagListener);
            
            GatewayConnectionLostListener connectionLostListener = 
                new GatewayConnectionLostListener(readerId, this);
            reader.setConnectionLostListener(connectionLostListener);
            
            // Obtener configuración por defecto
            Settings settings = reader.queryDefaultSettings();
            
            // Configurar modo básico
            settings.setReaderMode(ReaderMode.AutoSetDenseReader);
            settings.setSearchMode(SearchMode.SingleTarget);
            settings.setSession((short) 1);
            
            // Configurar reporte
            ReportConfig report = settings.getReport();
            report.setMode(ReportMode.Individual);
            report.setIncludeAntennaPortNumber(true);
            report.setIncludePeakRssi(true);
            report.setIncludeLastSeenTime(true);
            report.setIncludeSeenCount(true);
            
            // Configurar antenas habilitadas
            List<Antenna> antennas = antennaRepository.findByReaderIdAndEnabledTrue(readerId);
            AntennaConfigGroup antennaConfigGroup = settings.getAntennas();
            antennaConfigGroup.disableAll();
            
            for (Antenna antenna : antennas) {
                short portNumber = antenna.getPortNumber();
                AntennaConfig antennaConfig = antennaConfigGroup.getAntenna(portNumber);
                
                if (antennaConfig != null) {
                    antennaConfig.setEnabled(true);
                    
                    // Configurar potencia
                    if (readerConfig.getUseDefaultPower() != null && readerConfig.getUseDefaultPower() 
                        && readerConfig.getDefaultTxPowerDbm() != null) {
                        antennaConfig.setIsMaxTxPower(false);
                        antennaConfig.setTxPowerinDbm(readerConfig.getDefaultTxPowerDbm());
                    } else if (antenna.getTxPowerDbm() != null) {
                        antennaConfig.setIsMaxTxPower(false);
                        if (readerConfig.getMaxTxPowerDbm() != null) {
                            double power = Math.min(antenna.getTxPowerDbm(), readerConfig.getMaxTxPowerDbm());
                            antennaConfig.setTxPowerinDbm(power);
                        } else {
                            antennaConfig.setTxPowerinDbm(antenna.getTxPowerDbm());
                        }
                    } else {
                        antennaConfig.setIsMaxTxPower(true);
                    }
                    
                    // Configurar sensibilidad
                    if (readerConfig.getDefaultRxSensitivityDbm() != null) {
                        antennaConfig.setIsMaxRxSensitivity(false);
                        antennaConfig.setRxSensitivityinDbm(readerConfig.getDefaultRxSensitivityDbm());
                    } else if (antenna.getRxSensitivityDbm() != null) {
                        antennaConfig.setIsMaxRxSensitivity(false);
                        antennaConfig.setRxSensitivityinDbm(antenna.getRxSensitivityDbm());
                    } else {
                        antennaConfig.setIsMaxRxSensitivity(true);
                    }
                }
            }
            
            // Aplicar configuración
            reader.applySettings(settings);
            
            // Actualizar estado en BD
            readerConfig.setIsConnected(true);
            readerConfig.setIsReading(false); // No iniciar lectura automáticamente, solo con sesiones
            readerRepository.save(readerConfig);
            
            log.info("Lector {} conectado y configurado exitosamente", readerId);
            
        } catch (OctaneSdkException e) {
            log.error("Error del SDK al conectar lector {}: {}", readerId, e.getMessage(), e);
            
            // Actualizar estado en BD
            readerConfig.setIsConnected(false);
            readerConfig.setIsReading(false);
            readerRepository.save(readerConfig);
            
            // Limpiar instancia si existe
            readers.remove(readerId);
            
        } catch (Exception e) {
            log.error("Error al conectar lector {}: {}", readerId, e.getMessage(), e);
            
            // Actualizar estado en BD
            readerConfig.setIsConnected(false);
            readerConfig.setIsReading(false);
            readerRepository.save(readerConfig);
            
            // Limpiar instancia si existe
            readers.remove(readerId);
        }
    }
    
    /**
     * Desconecta un lector
     * @param readerId ID del lector
     */
    public void disconnectReader(String readerId) {
        log.info("Desconectando lector {}", readerId);
        
        try {
            ImpinjReader reader = readers.get(readerId);
            if (reader != null && reader.isConnected()) {
                reader.stop(); // Detener lectura primero
                reader.disconnect();
                log.info("Lector {} desconectado", readerId);
            }
            
            // Limpiar instancias
            readers.remove(readerId);
            
            // Actualizar estado en BD
            Reader readerConfig = readerRepository.findById(readerId).orElse(null);
            if (readerConfig != null) {
                readerConfig.setIsConnected(false);
                readerConfig.setIsReading(false);
                readerRepository.save(readerConfig);
            }
            
        } catch (Exception e) {
            log.error("Error al desconectar lector {}: {}", readerId, e.getMessage(), e);
        }
    }
    
    /**
     * Maneja pérdida de conexión con un lector
     * @param readerId ID del lector
     */
    public void handleConnectionLost(String readerId) {
        log.warn("Manejando pérdida de conexión con lector {}", readerId);
        
        // Limpiar instancias
        readers.remove(readerId);
        
        // Actualizar estado en BD
        Reader readerConfig = readerRepository.findById(readerId).orElse(null);
        if (readerConfig != null) {
            readerConfig.setIsConnected(false);
            readerConfig.setIsReading(false);
            readerRepository.save(readerConfig);
        }
    }
}
