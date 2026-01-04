package com.rfidgateway.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SessionService {
    
    @Value("${gateway.sessions.max-duration-minutes:60}")
    private Integer defaultMaxDurationMinutes;
    
    // Sesiones activas: sessionId -> ReadingSession
    private final Map<String, ReadingSession> activeSessions = new ConcurrentHashMap<>();
    
    // Sesiones completadas: sessionId -> ReadingSession (para consulta temporal)
    private final Map<String, ReadingSession> completedSessions = new ConcurrentHashMap<>();
    
    // Mapeo de readerId -> sessionId activa (para verificar que no haya duplicados)
    private final Map<String, String> readerToActiveSession = new ConcurrentHashMap<>();
    
    // Mapeo de groupId -> sessionId activa (para verificar que no haya duplicados)
    private final Map<String, String> groupToActiveSession = new ConcurrentHashMap<>();
    
    /**
     * Inicia una nueva sesión para un lector
     * @param readerId ID del lector
     * @param maxDurationMinutes Duración máxima en minutos (null para usar el valor por defecto)
     */
    public ReadingSession startSession(String readerId, Integer maxDurationMinutes) {
        // Verificar que no haya sesión activa para este lector
        if (hasActiveSession(readerId)) {
            String existingSessionId = readerToActiveSession.get(readerId);
            ReadingSession existingSession = activeSessions.get(existingSessionId);
            String message = String.format(
                "El lector %s ya está activo en otra sesión (ID: %s). " +
                "La sesión actual debe detenerse antes de iniciar una nueva. " +
                "Iniciada: %s",
                readerId, 
                existingSessionId,
                existingSession != null ? existingSession.getStartTime().toString() : "N/A"
            );
            throw new IllegalStateException(message);
        }
        
        // Usar valor por defecto si no se especifica
        Integer duration = maxDurationMinutes != null ? maxDurationMinutes : defaultMaxDurationMinutes;
        
        // Crear nueva sesión
        String sessionId = UUID.randomUUID().toString();
        ReadingSession session = new ReadingSession(sessionId, readerId, duration);
        
        // Almacenar
        activeSessions.put(sessionId, session);
        readerToActiveSession.put(readerId, sessionId);
        
        log.info("Sesión {} iniciada para lector {} (duración máxima: {} minutos)", 
                sessionId, readerId, duration);
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
        
        // Limpiar mapeos
        if (session.getReaderId() != null) {
            readerToActiveSession.remove(session.getReaderId());
        }
        if (session.getGroupId() != null) {
            groupToActiveSession.remove(session.getGroupId());
            // Limpiar mapeos de todos los lectores del grupo
            if (session.getReaderIds() != null) {
                session.getReaderIds().forEach(readerToActiveSession::remove);
            }
        }
        
        String target = session.getGroupId() != null ? 
            "grupo " + session.getGroupId() : 
            "lector " + session.getReaderId();
        
        log.info("Sesión {} detenida para {}. EPCs detectados: {}", 
                sessionId, target, session.getEpcCount());
        
        return session;
    }
    
    /**
     * Inicia una nueva sesión para un grupo de lectores
     * @param groupId ID del grupo
     * @param readerIds Lista de IDs de lectores
     * @param maxDurationMinutes Duración máxima en minutos (null para usar el valor por defecto)
     */
    public ReadingSession startGroupSession(String groupId, List<String> readerIds, Integer maxDurationMinutes) {
        // Verificar que no haya sesión activa para este grupo
        if (groupToActiveSession.containsKey(groupId)) {
            String existingSessionId = groupToActiveSession.get(groupId);
            ReadingSession existingSession = activeSessions.get(existingSessionId);
            String message = String.format(
                "El grupo %s ya está activo en otra sesión (ID: %s). " +
                "La sesión actual debe detenerse antes de iniciar una nueva. " +
                "Iniciada: %s",
                groupId,
                existingSessionId,
                existingSession != null ? existingSession.getStartTime().toString() : "N/A"
            );
            throw new IllegalStateException(message);
        }
        
        // Verificar que ninguno de los lectores tenga sesión activa
        for (String readerId : readerIds) {
            if (hasActiveSession(readerId)) {
                String existingSessionId = readerToActiveSession.get(readerId);
                ReadingSession existingSession = activeSessions.get(existingSessionId);
                String message = String.format(
                    "El lector %s ya está activo en otra sesión (ID: %s). " +
                    "La sesión actual debe detenerse antes de iniciar una nueva. " +
                    "Iniciada: %s",
                    readerId,
                    existingSessionId,
                    existingSession != null ? existingSession.getStartTime().toString() : "N/A"
                );
                throw new IllegalStateException(message);
            }
        }
        
        // Usar valor por defecto si no se especifica
        Integer duration = maxDurationMinutes != null ? maxDurationMinutes : defaultMaxDurationMinutes;
        
        // Crear nueva sesión
        String sessionId = UUID.randomUUID().toString();
        ReadingSession session = new ReadingSession(sessionId, groupId, readerIds, duration);
        
        // Almacenar
        activeSessions.put(sessionId, session);
        groupToActiveSession.put(groupId, sessionId);
        
        // Mapear cada lector a la sesión
        readerIds.forEach(readerId -> readerToActiveSession.put(readerId, sessionId));
        
        log.info("Sesión {} iniciada para grupo {} con {} lector(es) (duración máxima: {} minutos)", 
                sessionId, groupId, readerIds.size(), duration);
        
        return session;
    }
    
    /**
     * Verifica si hay una sesión activa para un lector
     */
    public boolean hasActiveSession(String readerId) {
        return readerToActiveSession.containsKey(readerId);
    }
    
    /**
     * Verifica si hay una sesión activa para un grupo
     */
    public boolean hasActiveGroupSession(String groupId) {
        return groupToActiveSession.containsKey(groupId);
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
    
    /**
     * Detiene automáticamente las sesiones que han expirado
     * Este método debe ser llamado periódicamente por un scheduled task
     * @return Lista de sessionIds que fueron detenidas
     */
    public List<String> stopExpiredSessions() {
        List<String> expiredSessionIds = activeSessions.values().stream()
            .filter(ReadingSession::isExpired)
            .map(ReadingSession::getSessionId)
            .collect(Collectors.toList());
        
        List<String> stoppedIds = new ArrayList<>();
        for (String sessionId : expiredSessionIds) {
            try {
                ReadingSession session = stopSession(sessionId);
                stoppedIds.add(sessionId);
                log.info("Sesión {} detenida automáticamente por expiración de tiempo (duración máxima: {} minutos)", 
                        sessionId, session.getMaxDurationMinutes());
            } catch (Exception e) {
                log.error("Error al detener sesión expirada {}: {}", sessionId, e.getMessage(), e);
            }
        }
        
        return stoppedIds;
    }
}

