package com.rfidgateway.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SessionService {

    private final Map<String, ReadingSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, ReadingSession> completedSessions = new ConcurrentHashMap<>();
    private final Map<String, String> readerToSession = new ConcurrentHashMap<>();

    public ReadingSession startSession(String readerId) {
        if (readerToSession.containsKey(readerId)) {
            throw new IllegalStateException("Ya existe una sesión activa para el lector: " + readerId);
        }
        String sessionId = UUID.randomUUID().toString();
        ReadingSession session = new ReadingSession(sessionId, readerId);
        activeSessions.put(sessionId, session);
        readerToSession.put(readerId, sessionId);
        log.info("Sesión {} iniciada para lector {}", sessionId, readerId);
        return session;
    }

    public Optional<ReadingSession> getSession(String sessionId) {
        ReadingSession session = activeSessions.get(sessionId);
        if (session == null) {
            session = completedSessions.get(sessionId);
        }
        return Optional.ofNullable(session);
    }

    public Optional<ReadingSession> stopSession(String sessionId) {
        ReadingSession session = activeSessions.remove(sessionId);
        if (session != null) {
            session.stop();
            readerToSession.remove(session.getReaderId());
            completedSessions.put(sessionId, session);
            log.info("Sesión {} detenida para lector {}", sessionId, session.getReaderId());
            return Optional.of(session);
        }
        return Optional.empty();
    }

    public boolean hasActiveSession(String readerId) {
        return readerToSession.containsKey(readerId);
    }

    public void addEpcToSession(String readerId, String epc) {
        String sessionId = readerToSession.get(readerId);
        if (sessionId != null) {
            ReadingSession session = activeSessions.get(sessionId);
            if (session != null) {
                session.addEpc(epc);
            }
        }
    }

    public List<ReadingSession> getActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }
}
