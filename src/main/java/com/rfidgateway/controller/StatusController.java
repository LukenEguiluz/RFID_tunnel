package com.rfidgateway.controller;

import com.rfidgateway.model.Reader;
import com.rfidgateway.reader.ReaderManager;
import com.rfidgateway.repository.ReaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
public class StatusController {
    
    @Autowired
    private ReaderRepository readerRepository;
    
    @Autowired
    private ReaderManager readerManager;
    
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
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}







