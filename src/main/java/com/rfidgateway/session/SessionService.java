package com.rfidgateway.session;

import com.rfidgateway.model.ReaderGroup;
import com.rfidgateway.repository.ReaderGroupRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SessionService {

    private final Map<String, ReadingSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, ReadingSession> completedSessions = new ConcurrentHashMap<>();
    private final Map<String, String> readerToSession = new ConcurrentHashMap<>();
    /** Sesiones de grupo: groupSessionId -> info (groupId, readerIds, readerSessionIds) */
    private final Map<String, GroupSessionInfo> activeGroupSessions = new ConcurrentHashMap<>();

    @Autowired
    private ReaderGroupRepository readerGroupRepository;

    @Data
    public static class GroupSessionInfo {
        private final String groupSessionId;
        private final String groupId;
        private final List<String> readerIds;
        private final List<String> readerSessionIds;
        private final java.time.LocalDateTime startTime;
    }

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

    /**
     * Indica si en la sesión activa de este lector ya se vio este EPC.
     * Sirve para deduplicar: durante la misma lectura cada EPC solo se envía una vez.
     */
    public boolean hasSeenInActiveSession(String readerId, String epc) {
        String sessionId = readerToSession.get(readerId);
        if (sessionId == null) {
            return false;
        }
        ReadingSession session = activeSessions.get(sessionId);
        return session != null && session.hasEpc(epc);
    }

    public List<ReadingSession> getActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }

    /**
     * Inicia una sesión de lectura para todos los lectores del grupo.
     * @return GroupSessionInfo con el groupSessionId a usar en stop y get
     */
    public GroupSessionInfo startGroupSession(String groupId) {
        ReaderGroup group = readerGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Grupo no encontrado: " + groupId));
        List<String> readerIds = group.getReaders() != null
            ? group.getReaders().stream().map(r -> r.getId()).collect(Collectors.toList())
            : Collections.emptyList();
        if (readerIds.isEmpty()) {
            throw new IllegalStateException("El grupo no tiene lectores: " + groupId);
        }
        for (String rid : readerIds) {
            if (readerToSession.containsKey(rid)) {
                throw new IllegalStateException("El lector " + rid + " ya tiene una sesión activa");
            }
        }
        List<String> sessionIds = new ArrayList<>();
        for (String readerId : readerIds) {
            ReadingSession s = startSession(readerId);
            sessionIds.add(s.getSessionId());
        }
        String groupSessionId = "grp-" + UUID.randomUUID().toString();
        GroupSessionInfo info = new GroupSessionInfo(
            groupSessionId, groupId, readerIds, sessionIds, java.time.LocalDateTime.now());
        activeGroupSessions.put(groupSessionId, info);
        log.info("Sesión de grupo {} iniciada para lectores {}", groupSessionId, readerIds);
        return info;
    }

    public boolean isGroupSession(String sessionId) {
        return sessionId != null && sessionId.startsWith("grp-") && activeGroupSessions.containsKey(sessionId);
    }

    public Optional<GroupSessionInfo> getGroupSessionInfo(String groupSessionId) {
        return Optional.ofNullable(activeGroupSessions.get(groupSessionId));
    }

    /**
     * Detiene la sesión de grupo: todas las sesiones de lectores del grupo.
     * No detiene los lectores (eso lo hace el controller).
     */
    public Optional<GroupSessionInfo> stopGroupSession(String groupSessionId) {
        GroupSessionInfo info = activeGroupSessions.remove(groupSessionId);
        if (info == null) {
            return Optional.empty();
        }
        for (String readerSessionId : info.getReaderSessionIds()) {
            stopSession(readerSessionId);
        }
        log.info("Sesión de grupo {} detenida", groupSessionId);
        return Optional.of(info);
    }

    /**
     * Vista agregada de una sesión de grupo activa (EPCs de todos los lectores del grupo).
     */
    public Optional<Map<String, Object>> getGroupSessionView(String groupSessionId) {
        GroupSessionInfo info = activeGroupSessions.get(groupSessionId);
        if (info == null) {
            return Optional.empty();
        }
        Set<String> allEpcs = new TreeSet<>();
        long totalReads = 0;
        for (String sid : info.getReaderSessionIds()) {
            ReadingSession s = activeSessions.get(sid);
            if (s != null) {
                allEpcs.addAll(s.getDetectedEpcs());
                totalReads += s.getTotalReads();
            }
        }
        Map<String, Object> view = new HashMap<>();
        view.put("sessionId", groupSessionId);
        view.put("groupId", info.getGroupId());
        view.put("readerIds", info.getReaderIds());
        view.put("status", "ACTIVE");
        view.put("startTime", info.getStartTime());
        view.put("epcs", new ArrayList<>(allEpcs));
        view.put("epcCount", allEpcs.size());
        view.put("totalReads", totalReads);
        return Optional.of(view);
    }

    /**
     * Reset forzado para un lector: detiene cualquier sesión activa (individual o de grupo) de ese lector.
     * @return información para que el controller detenga físicamente el/los lectores; empty si no había sesión.
     */
    public Optional<ForceResetResult> forceResetReader(String readerId) {
        String sessionId = readerToSession.get(readerId);
        if (sessionId == null) {
            return Optional.empty();
        }
        // Si el lector pertenece a una sesión de grupo, detener toda la sesión de grupo
        String groupSessionToStop = null;
        for (Map.Entry<String, GroupSessionInfo> e : activeGroupSessions.entrySet()) {
            if (e.getValue().getReaderIds().contains(readerId)) {
                groupSessionToStop = e.getKey();
                break;
            }
        }
        if (groupSessionToStop != null) {
            Optional<GroupSessionInfo> stopped = stopGroupSession(groupSessionToStop);
            if (stopped.isPresent()) {
                return Optional.of(new ForceResetResult(true, stopped.get().getReaderIds(), groupSessionToStop));
            }
        }
        // Sesión individual
        Optional<ReadingSession> stopped = stopSession(sessionId);
        return stopped.isPresent() ? Optional.of(new ForceResetResult(false, List.of(readerId), null)) : Optional.empty();
    }

    /**
     * Reset forzado para un grupo: detiene la sesión de grupo si existe, y cualquier sesión individual de lectores del grupo.
     * @return lista de readerIds que tenían sesión y fueron detenidos (para que el controller llame stopReader).
     */
    public List<String> forceResetGroup(String groupId) {
        List<String> stoppedReaderIds = new ArrayList<>();
        // Buscar sesión de grupo activa para este groupId
        String toRemove = null;
        for (Map.Entry<String, GroupSessionInfo> e : activeGroupSessions.entrySet()) {
            if (groupId.equals(e.getValue().getGroupId())) {
                toRemove = e.getKey();
                break;
            }
        }
        if (toRemove != null) {
            stopGroupSession(toRemove).ifPresent(info -> stoppedReaderIds.addAll(info.getReaderIds()));
        }
        // También limpiar sesiones individuales de lectores que pertenecen al grupo
        ReaderGroup group = readerGroupRepository.findById(groupId).orElse(null);
        if (group != null && group.getReaders() != null) {
            for (var r : group.getReaders()) {
                String rid = r.getId();
                if (readerToSession.containsKey(rid)) {
                    stopSession(readerToSession.get(rid));
                    if (!stoppedReaderIds.contains(rid)) {
                        stoppedReaderIds.add(rid);
                    }
                }
            }
        }
        return stoppedReaderIds;
    }

    /**
     * Reset forzado total: detiene todas las sesiones (grupos e individuales).
     * @return lista de readerIds que tenían sesión (para que el controller llame stopReader en cada uno).
     */
    public List<String> forceResetAll() {
        List<String> stoppedReaderIds = new ArrayList<>();
        // Detener todas las sesiones de grupo
        for (String groupSessionId : new ArrayList<>(activeGroupSessions.keySet())) {
            stopGroupSession(groupSessionId).ifPresent(info -> stoppedReaderIds.addAll(info.getReaderIds()));
        }
        // Detener todas las sesiones individuales restantes
        for (ReadingSession s : new ArrayList<>(activeSessions.values())) {
            stopSession(s.getSessionId());
            stoppedReaderIds.add(s.getReaderId());
        }
        return stoppedReaderIds;
    }

    @Data
    public static class ForceResetResult {
        private final boolean wasGroupSession;
        private final List<String> readerIdsToStop;
        private final String groupSessionId;
    }
}
