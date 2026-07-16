package com.rfidgateway.controller;

import com.rfidgateway.model.Antenna;
import com.rfidgateway.reader.ReaderManager;
import com.rfidgateway.repository.AntennaRepository;
import com.rfidgateway.repository.ReaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/antennas")
public class AntennaController {
    
    @Autowired
    private AntennaRepository antennaRepository;
    
    @Autowired
    private ReaderRepository readerRepository;
    
    @Autowired(required = false)
    private ReaderManager readerManager;

    @PostMapping
    public ResponseEntity<?> createAntenna(@RequestBody Antenna body) {
        if (body.getReaderId() == null || body.getReaderId().isBlank()
            || body.getPortNumber() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "readerId y portNumber son obligatorios"));
        }
        if (!readerRepository.existsById(body.getReaderId())) {
            return ResponseEntity.status(404).body(Map.of("error", "Lector no encontrado"));
        }
        String id = body.getReaderId() + "-antenna-" + body.getPortNumber();
        if (body.getId() != null && !body.getId().isBlank()) {
            id = body.getId().trim();
        }
        if (antennaRepository.existsById(id)
            || antennaRepository.findByReaderIdAndPortNumber(body.getReaderId(), body.getPortNumber()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "Ya existe antena para ese lector y puerto"));
        }
        Antenna a = new Antenna();
        a.setId(id);
        a.setReaderId(body.getReaderId());
        a.setPortNumber(body.getPortNumber());
        a.setName(body.getName() != null && !body.getName().isBlank()
            ? body.getName().trim()
            : "Puerto " + body.getPortNumber());
        a.setEnabled(body.getEnabled() != null ? body.getEnabled() : true);
        a.setTxPowerDbm(body.getTxPowerDbm());
        a.setRxSensitivityDbm(body.getRxSensitivityDbm());
        Antenna saved = antennaRepository.save(a);
        if (readerManager != null) {
            readerManager.resetAntennas(body.getReaderId());
        }
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Antenna>> getAllAntennas() {
        List<Antenna> antennas = antennaRepository.findAll();
        return ResponseEntity.ok(antennas);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Antenna> getAntenna(@PathVariable String id) {
        return antennaRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/reader/{readerId}")
    public ResponseEntity<List<Antenna>> getAntennasByReader(@PathVariable String readerId) {
        if (!readerRepository.existsById(readerId)) {
            return ResponseEntity.notFound().build();
        }
        
        List<Antenna> antennas = antennaRepository.findByReaderIdOrderByPortNumberAsc(readerId);
        return ResponseEntity.ok(antennas);
    }
    
    /**
     * Reset de antena por ID.
     * Reinicia la configuración de antenas del lector al que pertenece esta antena.
     * Útil cuando se quiere resetear por antena específica sin conocer el readerId.
     */
    @PostMapping("/{id}/reset")
    public ResponseEntity<?> resetAntenna(@PathVariable String id) {
        return antennaRepository.findById(id)
            .map(antenna -> {
                String readerId = antenna.getReaderId();
                if (readerManager != null) {
                    try {
                        readerManager.resetAntennas(readerId);
                        log.info("Reset de antena {} (lector {}) ejecutado correctamente", id, readerId);
                        return ResponseEntity.ok(Map.of(
                            "message", "Antenna configuration reset",
                            "antennaId", id,
                            "readerId", readerId
                        ));
                    } catch (Exception e) {
                        log.error("Error al resetear antena {}: {}", id, e.getMessage());
                        return ResponseEntity.status(500)
                            .body(Map.of("error", "Error al resetear: " + e.getMessage()));
                    }
                } else {
                    return ResponseEntity.status(503)
                        .body(Map.of("error", "ReaderManager no disponible"));
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Actualizar antena (habilitar/deshabilitar, potencia, sensibilidad).
     * Requiere POST /api/readers/{readerId}/antennas/reset o restart del lector para aplicar cambios.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAntenna(@PathVariable String id, @RequestBody Map<String, Object> request) {
        return antennaRepository.findById(id)
            .map(antenna -> {
                if (request.containsKey("enabled")) {
                    antenna.setEnabled(Boolean.valueOf(request.get("enabled").toString()));
                }
                if (request.containsKey("name")) {
                    antenna.setName((String) request.get("name"));
                }
                if (request.containsKey("txPowerDbm") || request.containsKey("tx_power_dbm")) {
                    Object val = request.getOrDefault("txPowerDbm", request.get("tx_power_dbm"));
                    antenna.setTxPowerDbm(Double.valueOf(val.toString()));
                }
                if (request.containsKey("rxSensitivityDbm") || request.containsKey("rx_sensitivity_dbm")) {
                    Object val = request.getOrDefault("rxSensitivityDbm", request.get("rx_sensitivity_dbm"));
                    antenna.setRxSensitivityDbm(Double.valueOf(val.toString()));
                }
                antennaRepository.save(antenna);
                log.info("Antena {} actualizada", id);
                return ResponseEntity.ok(antenna);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}




