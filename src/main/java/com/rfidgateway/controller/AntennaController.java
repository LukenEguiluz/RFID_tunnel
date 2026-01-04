package com.rfidgateway.controller;

import com.rfidgateway.model.Antenna;
import com.rfidgateway.model.Reader;
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
        
        List<Antenna> antennas = antennaRepository.findByReaderId(readerId);
        return ResponseEntity.ok(antennas);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAntenna(@PathVariable String id, @RequestBody Antenna antennaUpdate) {
        return antennaRepository.findById(id)
            .map(antenna -> {
                // Actualizar campos permitidos
                if (antennaUpdate.getName() != null) {
                    antenna.setName(antennaUpdate.getName());
                }
                if (antennaUpdate.getEnabled() != null) {
                    antenna.setEnabled(antennaUpdate.getEnabled());
                }
                if (antennaUpdate.getTxPowerDbm() != null) {
                    antenna.setTxPowerDbm(antennaUpdate.getTxPowerDbm());
                }
                if (antennaUpdate.getRxSensitivityDbm() != null) {
                    antenna.setRxSensitivityDbm(antennaUpdate.getRxSensitivityDbm());
                }
                if (antennaUpdate.getReadDurationSeconds() != null) {
                    antenna.setReadDurationSeconds(antennaUpdate.getReadDurationSeconds());
                }
                
                antennaRepository.save(antenna);
                
                // Actualizar contador de antenas conectadas del lector
                updateConnectedAntennasCount(antenna.getReaderId());
                
                return ResponseEntity.ok(Map.of(
                    "message", "Antena actualizada exitosamente",
                    "antenna", antenna
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private void updateConnectedAntennasCount(String readerId) {
        try {
            Reader reader = readerRepository.findById(readerId).orElse(null);
            if (reader != null) {
                // Contar antenas habilitadas (enabled = true)
                int count = antennaRepository.findByReaderIdAndEnabledTrue(readerId).size();
                reader.setConnectedAntennasCount(count);
                readerRepository.save(reader);
            }
        } catch (Exception e) {
            log.error("Error al actualizar contador de antenas para lector {}: {}", readerId, e.getMessage());
        }
    }
}




