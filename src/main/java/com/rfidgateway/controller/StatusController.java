package com.rfidgateway.controller;

import com.rfidgateway.model.Reader;
import com.rfidgateway.repository.ReaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
public class StatusController {
    
    @Autowired
    private DataSource dataSource;

    @Autowired
    private ReaderRepository readerRepository;
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        List<Reader> readers = readerRepository.findAll();
        
        long totalReaders = readers.size();
        long connectedReaders = readers.stream()
            .filter(r -> r.getIsConnected() != null && r.getIsConnected())
            .count();
        long readingReaders = readers.stream()
            .filter(r -> r.getIsReading() != null && r.getIsReading())
            .count();
        
        Map<String, Object> status = new HashMap<>();
        status.put("totalReaders", totalReaders);
        status.put("connectedReaders", connectedReaders);
        status.put("readingReaders", readingReaders);
        status.put("readers", readers.stream()
            .map(r -> Map.of(
                "id", r.getId(),
                "name", r.getName(),
                "connected", r.getIsConnected() != null ? r.getIsConnected() : false,
                "reading", r.getIsReading() != null ? r.getIsReading() : false
            ))
            .collect(Collectors.toList()));
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Incluye comprobación JDBC para que Docker/K8s no marquen el contenedor como sano
     * si PostgreSQL no responde (evita formularios que fallan al guardar).
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "UP");
        try (Connection c = dataSource.getConnection()) {
            if (c.isValid(5)) {
                body.put("database", "UP");
                return ResponseEntity.ok(body);
            }
            body.put("database", "DOWN");
            return ResponseEntity.status(503).body(body);
        } catch (Exception e) {
            log.warn("Health DB: {}", e.getMessage());
            body.put("database", "DOWN");
            body.put("detail", e.getClass().getSimpleName());
            return ResponseEntity.status(503).body(body);
        }
    }

    /** Comprueba que la API responde (útil para verificar que el código desplegado es el nuevo). */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of("ok", true, "message", "Gateway API OK"));
    }
}







