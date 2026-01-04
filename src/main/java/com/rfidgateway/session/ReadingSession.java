package com.rfidgateway.session;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class ReadingSession {
    private String sessionId;
    private String readerId;      // Para sesiones de un solo lector (legacy)
    private String groupId;       // Para sesiones de grupo
    private List<String> readerIds;  // Lista de lectores en la sesión
    private SessionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxDurationMinutes;  // Duración máxima en minutos (null = sin límite)
    private LocalDateTime expirationTime;  // Tiempo de expiración calculado
    private Set<String> detectedEpcs;  // EPCs únicos detectados
    private AtomicInteger totalReads;    // Total de lecturas (puede haber duplicados)
    
    // Constructor para sesión de un solo lector (legacy)
    public ReadingSession(String sessionId, String readerId, Integer maxDurationMinutes) {
        this.sessionId = sessionId;
        this.readerId = readerId;
        this.groupId = null;
        this.readerIds = new ArrayList<>();
        this.readerIds.add(readerId);
        this.status = SessionStatus.ACTIVE;
        this.startTime = LocalDateTime.now();
        this.endTime = null;
        this.maxDurationMinutes = maxDurationMinutes;
        this.expirationTime = maxDurationMinutes != null && maxDurationMinutes > 0 
            ? this.startTime.plusMinutes(maxDurationMinutes) 
            : null;
        this.detectedEpcs = ConcurrentHashMap.newKeySet();
        this.totalReads = new AtomicInteger(0);
    }
    
    // Constructor para sesión de grupo
    public ReadingSession(String sessionId, String groupId, List<String> readerIds, Integer maxDurationMinutes) {
        this.sessionId = sessionId;
        this.readerId = null;
        this.groupId = groupId;
        this.readerIds = new ArrayList<>(readerIds);
        this.status = SessionStatus.ACTIVE;
        this.startTime = LocalDateTime.now();
        this.endTime = null;
        this.maxDurationMinutes = maxDurationMinutes;
        this.expirationTime = maxDurationMinutes != null && maxDurationMinutes > 0 
            ? this.startTime.plusMinutes(maxDurationMinutes) 
            : null;
        this.detectedEpcs = ConcurrentHashMap.newKeySet();
        this.totalReads = new AtomicInteger(0);
    }
    
    /**
     * Agrega un EPC a la sesión (solo si es único)
     */
    public void addEpc(String epc) {
        if (epc != null && !epc.isEmpty()) {
            detectedEpcs.add(epc);
            totalReads.incrementAndGet();
        }
    }
    
    /**
     * Detiene la sesión
     */
    public void stop() {
        this.status = SessionStatus.STOPPED;
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * Completa la sesión
     */
    public void complete() {
        this.status = SessionStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * Obtiene el número de EPCs únicos
     */
    public int getEpcCount() {
        return detectedEpcs.size();
    }
    
    /**
     * Obtiene el total de lecturas
     */
    public int getTotalReads() {
        return totalReads.get();
    }
    
    /**
     * Verifica si la sesión ha expirado
     */
    public boolean isExpired() {
        if (expirationTime == null) {
            return false; // Sin límite de tiempo
        }
        return LocalDateTime.now().isAfter(expirationTime);
    }
}

