package com.rfidgateway.controller;

import com.rfidgateway.model.TagEvent;
import com.rfidgateway.repository.TagEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/events")
public class EventController {
    
    @Autowired
    private TagEventRepository tagEventRepository;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getEvents(
            @RequestParam(required = false) String epc,
            @RequestParam(required = false) String reader,
            @RequestParam(required = false) String antenna,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("detectedAt").descending());
        Page<TagEvent> events;
        
        if (epc != null) {
            events = tagEventRepository.findByEpc(epc, pageable);
        } else if (reader != null) {
            events = tagEventRepository.findByReaderId(reader, pageable);
        } else if (antenna != null) {
            events = tagEventRepository.findByAntennaId(antenna, pageable);
        } else if (from != null && to != null) {
            events = tagEventRepository.findByDetectedAtBetween(from, to, pageable);
        } else {
            events = tagEventRepository.findAllByOrderByDetectedAtDesc(pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("events", events.getContent());
        response.put("totalElements", events.getTotalElements());
        response.put("totalPages", events.getTotalPages());
        response.put("currentPage", page);
        response.put("size", size);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatestEvents(
            @RequestParam(defaultValue = "20") int limit) {
        
        Pageable pageable = PageRequest.of(0, limit, Sort.by("detectedAt").descending());
        Page<TagEvent> events = tagEventRepository.findAllByOrderByDetectedAtDesc(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("events", events.getContent());
        response.put("count", events.getNumberOfElements());
        
        return ResponseEntity.ok(response);
    }
}






