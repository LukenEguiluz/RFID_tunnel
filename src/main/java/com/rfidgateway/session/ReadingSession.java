package com.rfidgateway.session;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ReadingSession {

    private final String sessionId;
    private final String readerId;
    private volatile SessionStatus status = SessionStatus.ACTIVE;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private final Set<String> detectedEpcs = ConcurrentHashMap.newKeySet();
    private volatile long totalReads = 0;

    public ReadingSession(String sessionId, String readerId) {
        this.sessionId = sessionId;
        this.readerId = readerId;
        this.startTime = LocalDateTime.now();
    }

    public void addEpc(String epc) {
        detectedEpcs.add(epc);
        totalReads++;
    }

    /** Indica si este EPC ya fue detectado en esta sesión (para deduplicar por sesión). */
    public boolean hasEpc(String epc) {
        return detectedEpcs.contains(epc);
    }

    public void stop() {
        this.status = SessionStatus.STOPPED;
        this.endTime = LocalDateTime.now();
    }

    public void complete() {
        this.status = SessionStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
    }

    public List<String> getEpcsSorted() {
        List<String> list = new ArrayList<>(detectedEpcs);
        Collections.sort(list);
        return list;
    }
}
