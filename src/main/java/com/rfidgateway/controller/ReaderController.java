package com.rfidgateway.controller;

import com.rfidgateway.model.Reader;
import com.rfidgateway.reader.ReaderManager;
import com.rfidgateway.repository.ReaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/readers")
public class ReaderController {
    
    @Autowired
    private ReaderRepository readerRepository;
    
    @Autowired
    private ReaderManager readerManager;
    
    @GetMapping
    public ResponseEntity<List<Reader>> getAllReaders() {
        List<Reader> readers = readerRepository.findAll();
        return ResponseEntity.ok(readers);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Reader> getReader(@PathVariable String id) {
        return readerRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getReaderStatus(@PathVariable String id) {
        return readerRepository.findById(id)
            .map(reader -> {
                Map<String, Object> status = Map.of(
                    "id", reader.getId(),
                    "name", reader.getName(),
                    "connected", reader.getIsConnected() != null ? reader.getIsConnected() : false,
                    "reading", reader.getIsReading() != null ? reader.getIsReading() : false,
                    "lastSeen", reader.getLastSeen() != null ? reader.getLastSeen().toString() : "N/A"
                );
                return ResponseEntity.ok(status);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, String>> startReader(@PathVariable String id) {
        Reader reader = readerRepository.findById(id).orElse(null);
        if (reader == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!reader.getEnabled()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Reader is disabled"));
        }
        
        readerManager.startReader(id);
        return ResponseEntity.ok(Map.of("message", "Reader started", "readerId", id));
    }
    
    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, String>> stopReader(@PathVariable String id) {
        Reader reader = readerRepository.findById(id).orElse(null);
        if (reader == null) {
            return ResponseEntity.notFound().build();
        }
        
        readerManager.stopReader(id);
        return ResponseEntity.ok(Map.of("message", "Reader stopped", "readerId", id));
    }
    
    @PostMapping("/{id}/reset")
    public ResponseEntity<Map<String, String>> resetReader(@PathVariable String id) {
        Reader reader = readerRepository.findById(id).orElse(null);
        if (reader == null) {
            return ResponseEntity.notFound().build();
        }
        
        readerManager.resetReader(id);
        return ResponseEntity.ok(Map.of("message", "Reader reset and reconnecting", "readerId", id));
    }
    
    @PostMapping("/{id}/reboot")
    public ResponseEntity<Map<String, String>> rebootReader(@PathVariable String id) {
        Reader reader = readerRepository.findById(id).orElse(null);
        if (reader == null) {
            return ResponseEntity.notFound().build();
        }
        
        readerManager.rebootReader(id);
        return ResponseEntity.ok(Map.of("message", "Reader reboot initiated, will reconnect in 5 seconds", "readerId", id));
    }
    
    @PostMapping("/{id}/antennas/reset")
    public ResponseEntity<Map<String, String>> resetAntennas(@PathVariable String id) {
        Reader reader = readerRepository.findById(id).orElse(null);
        if (reader == null) {
            return ResponseEntity.notFound().build();
        }
        
        readerManager.resetAntennas(id);
        return ResponseEntity.ok(Map.of("message", "Antennas configuration reset", "readerId", id));
    }
}


