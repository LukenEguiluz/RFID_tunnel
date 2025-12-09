package com.rfidgateway.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SessionService {
    
    // Sesiones activas: sessionId -> ReadingSession
    private final Map<String, ReadingSession> activeSessions = new ConcurrentHashMap<>();
    
    // Sesiones completadas: sessionId -> ReadingSession (para consulta temporal)
    private final Map<String, ReadingSession> completedSessions = new ConcurrentHashMap<>();
    
    // Mapeo de readerId -> sessionId activa (para verificar que no haya duplicados)
    private final Map<String, String> readerToActiveSession = new ConcurrentHashMap<>();
    
    /**
     * Inicia una nueva sesión para un lector
     */
    public ReadingSession startSession(String readerId) {
        // Verificar que no haya sesión activa para este lector
        if (hasActiveSession(readerId)) {
            String existingSessionId = readerToActiveSession.get(readerId);
            throw new IllegalStateException(
                String.format("Ya existe una sesión activa para el lector %s: %s", readerId, existingSessionId)
            );
        }
        
        // Crear nueva sesión
        String sessionId = UUID.randomUUID().toString();
        ReadingSession session = new ReadingSession(sessionId, readerId);
        
        // Almacenar
        activeSessions.put(sessionId, session);
        readerToActiveSession.put(readerId, sessionId);
        
        log.info("Sesión {} iniciada para lector {}", sessionId, readerId);
        return session;
    }
    
    /**
     * Obtiene una sesión por ID
     */
    public ReadingSession getSession(String sessionId) {
        ReadingSession session = activeSessions.get(sessionId);
        if (session == null) {
            session = completedSessions.get(sessionId);
        }
        return session;
    }
    
    /**
     * Obtiene la sesión activa de un lector
     */
    public ReadingSession getActiveSessionByReader(String readerId) {
        String sessionId = readerToActiveSession.get(readerId);
        if (sessionId == null) {
            return null;
        }
        return activeSessions.get(sessionId);
    }
    
    /**
     * Detiene una sesión
     */
    public ReadingSession stopSession(String sessionId) {
        ReadingSession session = activeSessions.remove(sessionId);
        if (session == null) {
            session = completedSessions.get(sessionId);
            if (session == null) {
                throw new IllegalArgumentException("Sesión no encontrada: " + sessionId);
            }
            // Ya estaba detenida
            return session;
        }
        
        // Detener sesión
        session.stop();
        
        // Mover a sesiones completadas
        completedSessions.put(sessionId, session);
        
        // Limpiar mapeo de lector
        readerToActiveSession.remove(session.getReaderId());
        
        log.info("Sesión {} detenida para lector {}. EPCs detectados: {}", 
                sessionId, session.getReaderId(), session.getEpcCount());
        
        return session;
    }
    
    /**
     * Verifica si hay una sesión activa para un lector
     */
    public boolean hasActiveSession(String readerId) {
        return readerToActiveSession.containsKey(readerId);
    }
    
    /**
     * Lista todas las sesiones activas
     */
    public List<ReadingSession> getActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }
    
    /**
     * Agrega un EPC a una sesión activa
     */
    public void addEpcToSession(String readerId, String epc) {
        ReadingSession session = getActiveSessionByReader(readerId);
        if (session != null) {
            session.addEpc(epc);
        }
    }
    
    /**
     * Limpia sesiones completadas antiguas (mayores a 1 hora)
     */
    public void cleanupOldSessions() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<String> toRemove = completedSessions.values().stream()
            .filter(session -> session.getEndTime() != null && session.getEndTime().isBefore(oneHourAgo))
            .map(ReadingSession::getSessionId)
            .collect(Collectors.toList());
        
        toRemove.forEach(completedSessions::remove);
        
        if (!toRemove.isEmpty()) {
            log.debug("Limpieza: {} sesiones antiguas eliminadas", toRemove.size());
        }
    }
}

