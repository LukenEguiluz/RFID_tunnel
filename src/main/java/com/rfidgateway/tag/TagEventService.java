package com.rfidgateway.tag;

import com.rfidgateway.controller.RealtimeEventController;
import com.rfidgateway.model.TagEvent;
import com.rfidgateway.repository.AntennaRepository;
import com.rfidgateway.repository.TagEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class TagEventService {
    
    @Autowired
    private TagEventRepository tagEventRepository;
    
    @Autowired
    private AntennaRepository antennaRepository;
    
    @Autowired
    private WebSocketEventService webSocketEventService;
    
    @Autowired(required = false)
    private RealtimeEventController realtimeEventController;
    
    @Transactional
    public void processTagEvent(String readerId, String epc, Short antennaPort, 
                                Double rssi, Double phase) {
        try {
            // Obtener ID de antena
            String antennaId = antennaRepository
                .findByReaderIdAndPortNumber(readerId, antennaPort)
                .map(antenna -> antenna.getId())
                .orElse(readerId + "-antenna-" + antennaPort);
            
            // Crear evento
            TagEvent event = new TagEvent();
            event.setEpc(epc);
            event.setReaderId(readerId);
            event.setAntennaId(antennaId);
            event.setAntennaPort(antennaPort);
            event.setRssi(rssi);
            event.setPhase(phase);
            event.setDetectedAt(LocalDateTime.now());
            
            // Guardar en BD
            tagEventRepository.save(event);
            
            // Notificar vía WebSocket
            webSocketEventService.notifyTagDetected(readerId, epc, antennaId, antennaPort, rssi, phase);
            
            // Notificar vía SSE (Server-Sent Events)
            if (realtimeEventController != null) {
                realtimeEventController.broadcastEvent(event);
            }
            
        } catch (Exception e) {
            log.error("Error al procesar evento de tag: {}", e.getMessage(), e);
        }
    }
}


