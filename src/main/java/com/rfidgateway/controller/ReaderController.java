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
    
    /**
     * Configura lectura intermitente para un lector
     * POST /api/readers/{id}/intermittent
     * Body: {
     *   "enabled": true,
     *   "readDurationSeconds": 5,
     *   "pauseDurationSeconds": 5
     * }
     */
    @PostMapping("/{id}/intermittent")
    public ResponseEntity<Map<String, Object>> configureIntermittentReading(
            @PathVariable String id,
            @RequestBody Map<String, Object> config) {
        Reader reader = readerRepository.findById(id).orElse(null);
        if (reader == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Actualizar configuración
        if (config.containsKey("enabled")) {
            reader.setIntermittentEnabled(Boolean.valueOf(config.get("enabled").toString()));
        }
        if (config.containsKey("readDurationSeconds")) {
            Integer readDuration = Integer.valueOf(config.get("readDurationSeconds").toString());
            if (readDuration < 1) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "readDurationSeconds debe ser mayor a 0"));
            }
            reader.setReadDurationSeconds(readDuration);
        }
        if (config.containsKey("pauseDurationSeconds")) {
            Integer pauseDuration = Integer.valueOf(config.get("pauseDurationSeconds").toString());
            if (pauseDuration < 1) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "pauseDurationSeconds debe ser mayor a 0"));
            }
            reader.setPauseDurationSeconds(pauseDuration);
        }
        
        readerRepository.save(reader);
        
        // Si el lector está conectado y leyendo, reiniciar con nueva configuración
        if (reader.getIsConnected() != null && reader.getIsConnected() 
            && reader.getIsReading() != null && reader.getIsReading()) {
            // Detener lectura actual (esto detendrá el ciclo intermitente si estaba activo)
            readerManager.stopReader(id);
            
            // Reiniciar con nueva configuración
            readerManager.startReader(id);
        }
        
        return ResponseEntity.ok(Map.of(
            "message", "Configuración de lectura intermitente actualizada. Reinicia el lector para aplicar cambios.",
            "readerId", id,
            "intermittentEnabled", reader.getIntermittentEnabled() != null ? reader.getIntermittentEnabled() : false,
            "readDurationSeconds", reader.getReadDurationSeconds() != null ? reader.getReadDurationSeconds() : 5,
            "pauseDurationSeconds", reader.getPauseDurationSeconds() != null ? reader.getPauseDurationSeconds() : 5
        ));
    }
    
    /**
     * Configura potencia de transmisión para un lector
     * POST /api/readers/{id}/power
     * Body: {
     *   "defaultTxPowerDbm": 20.0,
     *   "maxTxPowerDbm": 30.0,
     *   "useDefaultPower": true
     * }
     */
    @PostMapping("/{id}/power")
    public ResponseEntity<Map<String, Object>> configurePower(
            @PathVariable String id,
            @RequestBody Map<String, Object> config) {
        Reader reader = readerRepository.findById(id).orElse(null);
        if (reader == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Actualizar configuración de potencia
        if (config.containsKey("defaultTxPowerDbm")) {
            Object powerObj = config.get("defaultTxPowerDbm");
            if (powerObj == null) {
                reader.setDefaultTxPowerDbm(null);
            } else {
                Double power = Double.valueOf(powerObj.toString());
                // Validar rango típico de potencia (generalmente entre 10 y 32.5 dBm para Impinj)
                if (power < 10.0 || power > 32.5) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "defaultTxPowerDbm debe estar entre 10.0 y 32.5 dBm"));
                }
                reader.setDefaultTxPowerDbm(power);
            }
        }
        
        if (config.containsKey("maxTxPowerDbm")) {
            Object powerObj = config.get("maxTxPowerDbm");
            if (powerObj == null) {
                reader.setMaxTxPowerDbm(null);
            } else {
                Double power = Double.valueOf(powerObj.toString());
                // Validar rango
                if (power < 10.0 || power > 32.5) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "maxTxPowerDbm debe estar entre 10.0 y 32.5 dBm"));
                }
                // Validar que maxTxPowerDbm >= defaultTxPowerDbm si ambos están definidos
                if (reader.getDefaultTxPowerDbm() != null && power < reader.getDefaultTxPowerDbm()) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "maxTxPowerDbm debe ser mayor o igual a defaultTxPowerDbm"));
                }
                reader.setMaxTxPowerDbm(power);
            }
        }
        
        if (config.containsKey("useDefaultPower")) {
            reader.setUseDefaultPower(Boolean.valueOf(config.get("useDefaultPower").toString()));
        }
        
        readerRepository.save(reader);
        
        // Si el lector está conectado, reiniciar para aplicar nueva configuración de potencia
        if (reader.getIsConnected() != null && reader.getIsConnected()) {
            log.info("Reiniciando lector {} para aplicar nueva configuración de potencia", id);
            readerManager.resetReader(id);
        }
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Configuración de potencia actualizada. El lector se reiniciará para aplicar cambios.");
        response.put("readerId", id);
        response.put("defaultTxPowerDbm", reader.getDefaultTxPowerDbm());
        response.put("maxTxPowerDbm", reader.getMaxTxPowerDbm());
        response.put("useDefaultPower", reader.getUseDefaultPower() != null ? reader.getUseDefaultPower() : false);
        
        return ResponseEntity.ok(response);
    }
}


