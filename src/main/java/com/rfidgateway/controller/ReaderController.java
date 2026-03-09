package com.rfidgateway.controller;

import com.rfidgateway.model.Reader;
import com.rfidgateway.reader.ReaderManager;
import com.rfidgateway.repository.ReaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/readers")
public class ReaderController {

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired(required = false)
    private ReaderManager readerManager;

    @GetMapping
    public ResponseEntity<List<Reader>> getAllReaders() {
        return ResponseEntity.ok(readerRepository.findAll());
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
            .map(r -> {
                Map<String, Object> status = new HashMap<>();
                status.put("id", r.getId());
                status.put("name", r.getName());
                status.put("connected", Boolean.TRUE.equals(r.getIsConnected()));
                status.put("reading", Boolean.TRUE.equals(r.getIsReading()));
                return ResponseEntity.<Map<String, Object>>ok(status);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Reader> createReader(@RequestBody Reader reader) {
        if (reader.getId() == null || reader.getId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (readerRepository.existsById(reader.getId())) {
            return ResponseEntity.status(409).body(null);
        }
        reader.setIsConnected(false);
        reader.setIsReading(false);
        return ResponseEntity.ok(readerRepository.save(reader));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reader> updateReader(@PathVariable String id, @RequestBody Reader reader) {
        return readerRepository.findById(id)
            .map(existing -> {
                existing.setName(reader.getName());
                existing.setHostname(reader.getHostname());
                if (reader.getEnabled() != null) {
                    existing.setEnabled(reader.getEnabled());
                }
                return ResponseEntity.ok(readerRepository.save(existing));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReader(@PathVariable String id) {
        if (!readerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (readerManager != null) {
            readerManager.disconnectReader(id);
        }
        readerRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Reader deleted", "readerId", id));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startReader(@PathVariable String id) {
        if (!readerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (readerManager != null) {
            readerManager.startReader(id);
            return ResponseEntity.ok(Map.of("message", "Reader started", "readerId", id));
        }
        return ResponseEntity.status(503).body(Map.of("error", "ReaderManager no disponible"));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<?> stopReader(@PathVariable String id) {
        if (!readerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (readerManager != null) {
            readerManager.stopReader(id);
            return ResponseEntity.ok(Map.of("message", "Reader stopped", "readerId", id));
        }
        return ResponseEntity.status(503).body(Map.of("error", "ReaderManager no disponible"));
    }

    @PostMapping("/{id}/reset")
    public ResponseEntity<?> resetReader(@PathVariable String id) {
        if (!readerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (readerManager != null) {
            readerManager.resetReader(id);
            return ResponseEntity.ok(Map.of("message", "Reader reset and reconnecting", "readerId", id));
        }
        return ResponseEntity.status(503).body(Map.of("error", "ReaderManager no disponible"));
    }

    @PostMapping("/{id}/reboot")
    public ResponseEntity<?> rebootReader(@PathVariable String id) {
        if (!readerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (readerManager != null) {
            readerManager.rebootReader(id);
            return ResponseEntity.ok(Map.of(
                "message", "Reader reboot initiated, will reconnect in 5 seconds",
                "readerId", id
            ));
        }
        return ResponseEntity.status(503).body(Map.of("error", "ReaderManager no disponible"));
    }

    @PostMapping("/{id}/antennas/reset")
    public ResponseEntity<?> resetReaderAntennas(@PathVariable String id) {
        if (!readerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (readerManager != null) {
            readerManager.resetAntennas(id);
            return ResponseEntity.ok(Map.of("message", "Antennas configuration reset", "readerId", id));
        }
        return ResponseEntity.status(503).body(Map.of("error", "ReaderManager no disponible"));
    }
}
