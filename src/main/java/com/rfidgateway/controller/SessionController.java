package com.rfidgateway.controller;

import com.rfidgateway.reader.ReaderManager;
import com.rfidgateway.repository.ReaderRepository;
import com.rfidgateway.session.ReadingSession;
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
        String readerId = request.get("readerId");
        if (readerId == null || readerId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "readerId es requerido"));
        }
        if (!readerRepository.existsById(readerId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lector no encontrado: " + readerId));
        }
        if (sessionService.hasActiveSession(readerId)) {
            return ResponseEntity.status(409).body(Map.of("error", "Ya existe una sesión activa para este lector"));
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
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId) {
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
