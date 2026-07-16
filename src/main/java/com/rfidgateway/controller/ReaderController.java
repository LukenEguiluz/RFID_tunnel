package com.rfidgateway.controller;

import com.rfidgateway.model.Reader;
import com.rfidgateway.model.ReaderBrand;
import com.rfidgateway.model.ReaderHardwareInfo;
import com.rfidgateway.model.ReaderOperationMode;
import com.rfidgateway.reader.ReaderManager;
import com.rfidgateway.repository.ReaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                status.put("operationMode", r.getOperationMode() != null ? r.getOperationMode().name() : ReaderOperationMode.TUNNEL.name());
                status.put("brand", r.getBrand() != null ? r.getBrand().name() : ReaderBrand.IMPINJ_OCTANE.name());
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
        reader.setOperationMode(ReaderOperationMode.TUNNEL);
        reader.setInventorySystemId(null);
        if (reader.getBrand() == null) {
            reader.setBrand(ReaderBrand.IMPINJ_OCTANE);
        }
        return ResponseEntity.ok(readerRepository.save(reader));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reader> updateReader(@PathVariable String id, @RequestBody Reader reader) {
        return readerRepository.findById(id)
            .map(existing -> {
                existing.setName(reader.getName());
                existing.setHostname(reader.getHostname());
                if (reader.getBrand() != null) {
                    existing.setBrand(reader.getBrand());
                }
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
        if (readerRepository.findById(id).map(r -> r.getOperationMode() == ReaderOperationMode.CONTINUOUS).orElse(false)) {
            return ResponseEntity.status(409).body(Map.of(
                "error", "Lector en modo inventario continuo; la lectura la orquesta el sistema (UI)."
            ));
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
        if (readerRepository.findById(id).map(r -> r.getOperationMode() == ReaderOperationMode.CONTINUOUS).orElse(false)) {
            return ResponseEntity.status(409).body(Map.of(
                "error", "Lector en modo inventario continuo; use la UI del sistema para desactivar el sistema."
            ));
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

    /** Capacidades del lector (Impinj conectado): modelo, firmware, número de antenas físicas. */
    @GetMapping("/{id}/hardware")
    public ResponseEntity<?> getHardware(@PathVariable String id) {
        if (!readerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (readerManager == null) {
            return ResponseEntity.status(503).body(Map.of("error", "ReaderManager no disponible"));
        }
        Optional<ReaderHardwareInfo> cap = readerManager.queryHardwareCapabilities(id);
        if (cap.isPresent()) {
            return ResponseEntity.ok(cap.get());
        }
        return ResponseEntity.status(404).body(Map.of(
            "error", "Sin datos: lector Impinj conectado requerido, o marca distinta de IMPINJ_OCTANE"));
    }

    /** Crea/actualiza filas de antenas en BD según el conteo del hardware Impinj y reaplica settings. */
    @PostMapping("/{id}/antennas/discover")
    public ResponseEntity<?> discoverAntennas(@PathVariable String id) {
        if (!readerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (readerManager == null) {
            return ResponseEntity.status(503).body(Map.of("error", "ReaderManager no disponible"));
        }
        try {
            int n = readerManager.discoverAndSyncAntennas(id);
            return ResponseEntity.ok(Map.of(
                "message", "Antenas sincronizadas",
                "readerId", id,
                "physicalAntennaPorts", n));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("discoverAntennas {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
