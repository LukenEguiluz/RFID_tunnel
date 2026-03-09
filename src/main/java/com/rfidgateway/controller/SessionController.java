package com.rfidgateway.controller;

import com.rfidgateway.reader.ReaderManager;
import com.rfidgateway.repository.ReaderRepository;
import com.rfidgateway.session.ReadingSession;
import com.rfidgateway.session.SessionService.ForceResetResult;
import com.rfidgateway.session.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @Autowired(required = false)
    private ReaderManager readerManager;

    @Autowired
    private ReaderRepository readerRepository;

    @PostMapping("/start")
    public ResponseEntity<?> startSession(@RequestBody Map<String, String> request) {
        String groupId = request != null ? request.get("groupId") : null;
        String readerId = request != null ? request.get("readerId") : null;

        if (groupId != null && !groupId.isBlank()) {
            return startGroupSession(groupId);
        }
        if (readerId == null || readerId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "readerId o groupId es requerido"));
        }
        if (!readerRepository.existsById(readerId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lector no encontrado: " + readerId));
        }
        if (sessionService.hasActiveSession(readerId)) {
            return ResponseEntity.status(409).body(Map.of(
                "error", "Ya existe una sesión activa para este lector",
                "forceReset", "POST /api/sessions/force-reset con body {\"readerId\": \"" + readerId + "\"}"
            ));
        }
        try {
            ReadingSession session = sessionService.startSession(readerId);
            if (readerManager != null) {
                readerManager.startReader(readerId);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getSessionId());
            response.put("readerId", session.getReaderId());
            response.put("status", session.getStatus().name());
            response.put("startTime", session.getStartTime());
            response.put("message", "Sesión iniciada exitosamente");
            return ResponseEntity.status(201).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of(
                "error", e.getMessage(),
                "forceReset", "POST /api/sessions/force-reset con body {\"readerId\": \"<id>\"} o {\"groupId\": \"<id>\"}"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<?> startGroupSession(String groupId) {
        try {
            SessionService.GroupSessionInfo info = sessionService.startGroupSession(groupId);
            if (readerManager != null) {
                for (String rid : info.getReaderIds()) {
                    readerManager.startReader(rid);
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", info.getGroupSessionId());
            response.put("groupId", info.getGroupId());
            response.put("readerIds", info.getReaderIds());
            response.put("status", "ACTIVE");
            response.put("startTime", info.getStartTime());
            response.put("message", "Sesión de grupo iniciada; lectura en todos los lectores del grupo");
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of(
                "error", e.getMessage(),
                "forceReset", "POST /api/sessions/force-reset con body {\"groupId\": \"" + groupId + "\"}"
            ));
        }
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId) {
        if (sessionService.isGroupSession(sessionId)) {
            return sessionService.getGroupSessionView(sessionId)
                .map(view -> ResponseEntity.ok(view))
                .orElse(ResponseEntity.notFound().build());
        }
        return sessionService.getSession(sessionId)
            .map(session -> {
                Map<String, Object> response = new HashMap<>();
                response.put("sessionId", session.getSessionId());
                response.put("readerId", session.getReaderId());
                response.put("status", session.getStatus().name());
                response.put("startTime", session.getStartTime());
                response.put("endTime", session.getEndTime());
                response.put("epcs", session.getEpcsSorted());
                response.put("epcCount", session.getDetectedEpcs().size());
                response.put("totalReads", session.getTotalReads());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{sessionId}/stop")
    public ResponseEntity<?> stopSession(@PathVariable String sessionId) {
        if (sessionService.isGroupSession(sessionId)) {
            return sessionService.stopGroupSession(sessionId)
                .map(info -> {
                    if (readerManager != null) {
                        for (String rid : info.getReaderIds()) {
                            readerManager.stopReader(rid);
                        }
                    }
                    Map<String, Object> response = new HashMap<>();
                    response.put("sessionId", info.getGroupSessionId());
                    response.put("groupId", info.getGroupId());
                    response.put("readerIds", info.getReaderIds());
                    response.put("status", "STOPPED");
                    response.put("message", "Sesión de grupo detenida");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
        }
        return sessionService.stopSession(sessionId)
            .map(session -> {
                if (readerManager != null) {
                    readerManager.stopReader(session.getReaderId());
                }
                Map<String, Object> response = new HashMap<>();
                response.put("sessionId", session.getSessionId());
                response.put("status", session.getStatus().name());
                response.put("endTime", session.getEndTime());
                response.put("epcs", session.getEpcsSorted());
                response.put("epcCount", session.getDetectedEpcs().size());
                response.put("totalReads", session.getTotalReads());
                response.put("message", "Sesión detenida exitosamente");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Reset forzado de sesión: detiene cualquier sesión activa y permite iniciar lectura de nuevo.
     * Body: { "readerId": "id" } para un lector, { "groupId": "id" } para un grupo, o {} para todo.
     * Resuelve 409 Conflict cuando ya hay sesión activa.
     */
    @PostMapping("/force-reset")
    public ResponseEntity<?> forceReset(@RequestBody(required = false) Map<String, String> request) {
        String readerId = request != null ? request.get("readerId") : null;
        String groupId = request != null ? request.get("groupId") : null;

        if (readerId != null && !readerId.isBlank()) {
            var result = sessionService.forceResetReader(readerId);
            if (result.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "No había sesión activa para este lector",
                    "readerId", readerId
                ));
            }
            ForceResetResult r = result.get();
            if (readerManager != null) {
                for (String rid : r.getReaderIdsToStop()) {
                    readerManager.stopReader(rid);
                }
            }
            return ResponseEntity.ok(Map.of(
                "message", "Sesión detenida. Ya puedes iniciar lectura.",
                "wasGroupSession", r.isWasGroupSession(),
                "stoppedReaderIds", r.getReaderIdsToStop()
            ));
        }

        if (groupId != null && !groupId.isBlank()) {
            List<String> stopped = sessionService.forceResetGroup(groupId);
            if (readerManager != null) {
                for (String rid : stopped) {
                    readerManager.stopReader(rid);
                }
            }
            return ResponseEntity.ok(Map.of(
                "message", "Sesiones del grupo detenidas. Ya puedes iniciar lectura.",
                "groupId", groupId,
                "stoppedReaderIds", stopped
            ));
        }

        // Reset total
        List<String> stopped = sessionService.forceResetAll();
        if (readerManager != null) {
            for (String rid : stopped) {
                readerManager.stopReader(rid);
            }
        }
        return ResponseEntity.ok(Map.of(
            "message", "Todas las sesiones detenidas. Ya puedes iniciar lectura.",
            "stoppedReaderIds", stopped
        ));
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveSessions() {
        List<Map<String, Object>> sessions = sessionService.getActiveSessions().stream()
            .map(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("sessionId", s.getSessionId());
                m.put("readerId", s.getReaderId());
                m.put("status", s.getStatus().name());
                m.put("startTime", s.getStartTime());
                m.put("epcCount", s.getDetectedEpcs().size());
                m.put("totalReads", s.getTotalReads());
                return m;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("sessions", sessions));
    }
}
