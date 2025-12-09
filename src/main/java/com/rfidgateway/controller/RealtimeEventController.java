package com.rfidgateway.controller;

import com.rfidgateway.model.TagEvent;
import com.rfidgateway.repository.TagEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping("/api/realtime")
public class RealtimeEventController {
    
    @Autowired
    private TagEventRepository tagEventRepository;
    
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            @RequestParam(required = false) String readerId,
            @RequestParam(required = false) String epc) {
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        
        // Enviar eventos recientes al conectar
        try {
            List<TagEvent> recentEvents = getRecentEvents(readerId, epc, 10);
            for (TagEvent event : recentEvents) {
                emitter.send(SseEmitter.event()
                    .name("tag")
                    .data(formatEvent(event)));
            }
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("{\"status\":\"connected\",\"message\":\"Stream iniciado\"}"));
        } catch (IOException e) {
            log.error("Error al enviar eventos iniciales: {}", e.getMessage());
        }
        
        // Limpiar cuando se desconecte
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("Cliente SSE desconectado. Clientes activos: {}", emitters.size());
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("Cliente SSE timeout. Clientes activos: {}", emitters.size());
        });
        
        emitter.onError((ex) -> {
            emitters.remove(emitter);
            log.debug("Error en SSE. Clientes activos: {}", emitters.size());
        });
        
        log.info("Nuevo cliente SSE conectado. Total clientes: {}", emitters.size());
        return emitter;
    }
    
    @GetMapping("/events/latest")
    public ResponseEntity<List<TagEvent>> getLatestEvents(
            @RequestParam(required = false) String readerId,
            @RequestParam(required = false) String epc,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<TagEvent> events = getRecentEvents(readerId, epc, limit);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Object> getStats(
            @RequestParam(required = false) String readerId) {
        
        LocalDateTime lastMinute = LocalDateTime.now().minusMinutes(1);
        long countLastMinute = readerId != null 
            ? tagEventRepository.countByReaderIdAndDetectedAtAfter(readerId, lastMinute)
            : tagEventRepository.countByDetectedAtAfter(lastMinute);
        
        LocalDateTime lastHour = LocalDateTime.now().minusHours(1);
        long countLastHour = readerId != null
            ? tagEventRepository.countByReaderIdAndDetectedAtAfter(readerId, lastHour)
            : tagEventRepository.countByDetectedAtAfter(lastHour);
        
        return ResponseEntity.ok(java.util.Map.of(
            "lastMinute", countLastMinute,
            "lastHour", countLastHour,
            "totalEvents", tagEventRepository.count(),
            "timestamp", LocalDateTime.now().toString()
        ));
    }
    
    private List<TagEvent> getRecentEvents(String readerId, String epc, int limit) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        if (readerId != null && epc != null) {
            return tagEventRepository.findTopByReaderIdAndEpcOrderByDetectedAtDesc(readerId, epc, pageable);
        } else if (readerId != null) {
            return tagEventRepository.findTopByReaderIdOrderByDetectedAtDesc(readerId, pageable);
        } else if (epc != null) {
            return tagEventRepository.findTopByEpcOrderByDetectedAtDesc(epc, pageable);
        } else {
            return tagEventRepository.findTopOrderByDetectedAtDesc(pageable);
        }
    }
    
    private String formatEvent(TagEvent event) {
        return String.format(
            "{\"epc\":\"%s\",\"readerId\":\"%s\",\"antennaId\":\"%s\",\"antennaPort\":%s,\"rssi\":%s,\"detectedAt\":\"%s\"}",
            event.getEpc(),
            event.getReaderId(),
            event.getAntennaId(),
            event.getAntennaPort() != null ? event.getAntennaPort() : "null",
            event.getRssi() != null ? event.getRssi() : "null",
            event.getDetectedAt()
        );
    }
    
    // Método público para que otros servicios puedan notificar eventos
    public void broadcastEvent(TagEvent event) {
        String data = formatEvent(event);
        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("tag")
                    .data(data));
                return false;
            } catch (IOException e) {
                log.debug("Error al enviar evento SSE: {}", e.getMessage());
                return true; // Remover emisor con error
            }
        });
    }
}

