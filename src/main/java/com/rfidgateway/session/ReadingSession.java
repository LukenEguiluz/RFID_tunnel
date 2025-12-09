package com.rfidgateway.session;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class ReadingSession {
    private String sessionId;
    private String readerId;
    private SessionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Set<String> detectedEpcs;  // EPCs únicos detectados
    private AtomicInteger totalReads;    // Total de lecturas (puede haber duplicados)
    
    public ReadingSession(String sessionId, String readerId) {
        this.sessionId = sessionId;
        this.readerId = readerId;
        this.status = SessionStatus.ACTIVE;
        this.startTime = LocalDateTime.now();
        this.endTime = null;
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
}

