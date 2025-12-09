package com.rfidgateway.controller;

import com.rfidgateway.reader.ReaderManager;
import com.rfidgateway.repository.ReaderRepository;
import com.rfidgateway.session.ReadingSession;
import com.rfidgateway.session.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    
    @Autowired
    private ReaderManager readerManager;
    
    @Autowired
    private ReaderRepository readerRepository;
    
    /**
     * Iniciar una nueva sesión de lectura
     * POST /api/sessions/start
     * Body: { "readerId": "reader-1" }
     */
    @PostMapping("/start")
    public ResponseEntity<?> startSession(@RequestBody Map<String, String> request) {
        String readerId = request.get("readerId");
        
        if (readerId == null || readerId.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "readerId es requerido"));
        }
        
        // Verificar que el lector exista
        if (!readerRepository.existsById(readerId)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Lector no encontrado: " + readerId));
        }
        
        // Verificar que el lector esté conectado
        var reader = readerRepository.findById(readerId);
        if (reader.isEmpty() || reader.get().getIsConnected() == null || !reader.get().getIsConnected()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "El lector no está conectado: " + readerId));
        }
        
        try {
            // Crear sesión
            ReadingSession session = sessionService.startSession(readerId);
            
            // Iniciar lectura en el lector
            readerManager.startSessionReading(readerId, session.getSessionId());
            
            // Respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getSessionId());
            response.put("readerId", session.getReaderId());
            response.put("status", session.getStatus().toString());
            response.put("startTime", session.getStartTime().toString());
            response.put("message", "Sesión iniciada exitosamente");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al iniciar sesión: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al iniciar sesión: " + e.getMessage()));
        }
    }
    
    /**
     * Consultar estado de una sesión
     * GET /api/sessions/{sessionId}
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId) {
        ReadingSession session = sessionService.getSession(sessionId);
        
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("readerId", session.getReaderId());
        response.put("status", session.getStatus().toString());
        response.put("startTime", session.getStartTime().toString());
        response.put("endTime", session.getEndTime() != null ? session.getEndTime().toString() : null);
        response.put("epcs", session.getDetectedEpcs().stream().sorted().collect(Collectors.toList()));
        response.put("epcCount", session.getEpcCount());
        response.put("totalReads", session.getTotalReads());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Detener una sesión
     * POST /api/sessions/{sessionId}/stop
     */
    @PostMapping("/{sessionId}/stop")
    public ResponseEntity<?> stopSession(@PathVariable String sessionId) {
        try {
            ReadingSession session = sessionService.stopSession(sessionId);
            
            // Detener lectura en el lector
            readerManager.stopSessionReading(session.getReaderId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getSessionId());
            response.put("status", session.getStatus().toString());
            response.put("endTime", session.getEndTime().toString());
            response.put("epcs", session.getDetectedEpcs().stream().sorted().collect(Collectors.toList()));
            response.put("epcCount", session.getEpcCount());
            response.put("totalReads", session.getTotalReads());
            response.put("message", "Sesión detenida exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al detener sesión: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al detener sesión: " + e.getMessage()));
        }
    }
    
    /**
     * Listar sesiones activas
     * GET /api/sessions/active
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveSessions() {
        List<ReadingSession> sessions = sessionService.getActiveSessions();
        
        List<Map<String, Object>> sessionList = sessions.stream().map(session -> {
            Map<String, Object> map = new HashMap<>();
            map.put("sessionId", session.getSessionId());
            map.put("readerId", session.getReaderId());
            map.put("status", session.getStatus().toString());
            map.put("startTime", session.getStartTime().toString());
            map.put("epcCount", session.getEpcCount());
            map.put("totalReads", session.getTotalReads());
            return map;
        }).collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessions", sessionList);
        response.put("count", sessionList.size());
        
        return ResponseEntity.ok(response);
    }
}

