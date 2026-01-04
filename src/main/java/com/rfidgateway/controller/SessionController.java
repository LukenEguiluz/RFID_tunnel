package com.rfidgateway.controller;

import com.rfidgateway.model.ReaderGroup;
import com.rfidgateway.reader.ReaderManager;
import com.rfidgateway.repository.ReaderGroupRepository;
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
    
    @Autowired
    private ReaderGroupRepository groupRepository;
    
    /**
     * Iniciar una nueva sesión de lectura
     * POST /api/sessions/start
     * Body: { "groupId": "group-1", "maxDurationMinutes": 30 }  o  
     *       { "readerId": "reader-1", "maxDurationMinutes": 30 } (legacy)
     */
    @PostMapping("/start")
    public ResponseEntity<?> startSession(@RequestBody Map<String, Object> request) {
        String groupId = request.get("groupId") != null ? request.get("groupId").toString() : null;
        String readerId = request.get("readerId") != null ? request.get("readerId").toString() : null;
        Integer maxDurationMinutes = null;
        
        // Extraer maxDurationMinutes si está presente
        if (request.get("maxDurationMinutes") != null) {
            try {
                Object durationObj = request.get("maxDurationMinutes");
                if (durationObj instanceof Number) {
                    maxDurationMinutes = ((Number) durationObj).intValue();
                } else if (durationObj instanceof String) {
                    maxDurationMinutes = Integer.parseInt((String) durationObj);
                }
                // Validar que sea positivo
                if (maxDurationMinutes != null && maxDurationMinutes <= 0) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "maxDurationMinutes debe ser un número positivo"));
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "maxDurationMinutes debe ser un número válido"));
            }
        }
        
        // Priorizar groupId sobre readerId
        if (groupId != null && !groupId.isEmpty()) {
            return startGroupSession(groupId, maxDurationMinutes);
        } else if (readerId != null && !readerId.isEmpty()) {
            return startSingleReaderSession(readerId, maxDurationMinutes);
        } else {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "groupId o readerId es requerido"));
        }
    }
    
    /**
     * Inicia sesión para un grupo de lectores
     */
    private ResponseEntity<?> startGroupSession(String groupId, Integer maxDurationMinutes) {
        // Verificar que el grupo exista
        var groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Grupo no encontrado: " + groupId));
        }
        
        ReaderGroup group = groupOpt.get();
        
        // Verificar que el grupo esté habilitado
        if (group.getEnabled() == null || !group.getEnabled()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "El grupo no está habilitado: " + groupId));
        }
        
        // Obtener lectores habilitados y conectados del grupo
        List<String> connectedReaderIds = group.getReaders().stream()
            .filter(reader -> reader.getEnabled() != null && reader.getEnabled())
            .filter(reader -> reader.getIsConnected() != null && reader.getIsConnected())
            .map(reader -> reader.getId())
            .collect(Collectors.toList());
        
        if (connectedReaderIds.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "No hay lectores conectados en el grupo: " + groupId));
        }
        
        try {
            // Crear sesión de grupo
            ReadingSession session = sessionService.startGroupSession(groupId, connectedReaderIds, maxDurationMinutes);
            
            // Iniciar lectura en todos los lectores del grupo
            for (String readerId : connectedReaderIds) {
                readerManager.startSessionReading(readerId, session.getSessionId());
            }
            
            // Respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getSessionId());
            response.put("groupId", session.getGroupId());
            response.put("readerIds", session.getReaderIds());
            response.put("readerCount", session.getReaderIds().size());
            response.put("status", session.getStatus().toString());
            response.put("startTime", session.getStartTime().toString());
            response.put("maxDurationMinutes", session.getMaxDurationMinutes());
            response.put("expirationTime", session.getExpirationTime() != null ? session.getExpirationTime().toString() : null);
            response.put("message", "Sesión de grupo iniciada exitosamente");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al iniciar sesión de grupo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al iniciar sesión de grupo: " + e.getMessage()));
        }
    }
    
    /**
     * Inicia sesión para un solo lector (legacy)
     */
    private ResponseEntity<?> startSingleReaderSession(String readerId, Integer maxDurationMinutes) {
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
            ReadingSession session = sessionService.startSession(readerId, maxDurationMinutes);
            
            // Iniciar lectura en el lector
            readerManager.startSessionReading(readerId, session.getSessionId());
            
            // Respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getSessionId());
            response.put("readerId", session.getReaderId());
            response.put("status", session.getStatus().toString());
            response.put("startTime", session.getStartTime().toString());
            response.put("maxDurationMinutes", session.getMaxDurationMinutes());
            response.put("expirationTime", session.getExpirationTime() != null ? session.getExpirationTime().toString() : null);
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
        if (session.getGroupId() != null) {
            response.put("groupId", session.getGroupId());
            response.put("readerIds", session.getReaderIds());
        } else {
            response.put("readerId", session.getReaderId());
        }
        response.put("status", session.getStatus().toString());
        response.put("startTime", session.getStartTime().toString());
        response.put("endTime", session.getEndTime() != null ? session.getEndTime().toString() : null);
        response.put("maxDurationMinutes", session.getMaxDurationMinutes());
        response.put("expirationTime", session.getExpirationTime() != null ? session.getExpirationTime().toString() : null);
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
            
            // Detener lectura en todos los lectores de la sesión
            if (session.getGroupId() != null && session.getReaderIds() != null) {
                // Sesión de grupo: detener en todos los lectores
                session.getReaderIds().forEach(readerManager::stopSessionReading);
            } else if (session.getReaderId() != null) {
                // Sesión de un solo lector (legacy)
                readerManager.stopSessionReading(session.getReaderId());
            }
            
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

