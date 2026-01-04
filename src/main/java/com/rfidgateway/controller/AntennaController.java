package com.rfidgateway.controller;

import com.rfidgateway.model.Antenna;
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
}




