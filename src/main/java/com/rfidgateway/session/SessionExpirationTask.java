package com.rfidgateway.session;

import com.rfidgateway.reader.ReaderManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SessionExpirationTask {
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private ReaderManager readerManager;
    
    /**
     * Tarea programada que verifica y detiene sesiones expiradas
     * Se ejecuta según el intervalo configurado (por defecto cada 30 segundos = 30000 ms)
     */
    @Scheduled(fixedDelayString = "${gateway.sessions.expiration-check-interval-ms:30000}", initialDelay = 60000)
    public void checkAndStopExpiredSessions() {
        try {
            List<String> expiredSessionIds = sessionService.stopExpiredSessions();
            
            if (!expiredSessionIds.isEmpty()) {
                log.info("Deteniendo {} sesión(es) expirada(s): {}", expiredSessionIds.size(), expiredSessionIds);
                
                // Para cada sesión expirada, detener la lectura en los lectores
                for (String sessionId : expiredSessionIds) {
                    ReadingSession session = sessionService.getSession(sessionId);
                    if (session != null) {
                        // Detener lectura en todos los lectores de la sesión
                        if (session.getGroupId() != null && session.getReaderIds() != null) {
                            // Sesión de grupo: detener en todos los lectores
                            session.getReaderIds().forEach(readerManager::stopSessionReading);
                        } else if (session.getReaderId() != null) {
                            // Sesión de un solo lector (legacy)
                            readerManager.stopSessionReading(session.getReaderId());
                        }
                        log.info("Lecturas detenidas para sesión expirada: {}", sessionId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al verificar sesiones expiradas: {}", e.getMessage(), e);
        }
    }
}

