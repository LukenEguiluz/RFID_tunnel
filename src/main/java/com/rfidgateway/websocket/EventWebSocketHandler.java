package com.rfidgateway.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class EventWebSocketHandler extends TextWebSocketHandler {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("Nueva conexión WebSocket: {}", session.getId());
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("Conexión WebSocket cerrada: {}", session.getId());
    }
    
    public void sendTagDetectedEvent(String readerId, String epc, String antennaId, 
                                    Short antennaPort, Double rssi, Double phase) {
        Map<String, Object> event = Map.of(
            "type", "TAG_DETECTED",
            "timestamp", Instant.now().toString(),
            "data", Map.of(
                "epc", epc,
                "readerId", readerId,
                "antennaId", antennaId,
                "antennaPort", antennaPort != null ? antennaPort : 0,
                "rssi", rssi != null ? rssi : 0.0,
                "phase", phase != null ? phase : 0.0
            )
        );
        
        sendToAll(event);
    }
    
    public void sendReaderDisconnectedEvent(String readerId, String readerName) {
        Map<String, Object> event = Map.of(
            "type", "READER_DISCONNECTED",
            "timestamp", Instant.now().toString(),
            "data", Map.of(
                "readerId", readerId,
                "readerName", readerName != null ? readerName : readerId,
                "reason", "Connection lost"
            )
        );
        
        sendToAll(event);
    }
    
    public void sendReaderReconnectedEvent(String readerId, String readerName) {
        Map<String, Object> event = Map.of(
            "type", "READER_RECONNECTED",
            "timestamp", Instant.now().toString(),
            "data", Map.of(
                "readerId", readerId,
                "readerName", readerName != null ? readerName : readerId
            )
        );
        
        sendToAll(event);
    }
    
    private void sendToAll(Map<String, Object> event) {
        String message;
        try {
            message = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Error al serializar evento: {}", e.getMessage());
            return;
        }
        
        TextMessage textMessage = new TextMessage(message);
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException e) {
                log.error("Error al enviar mensaje WebSocket: {}", e.getMessage());
            }
        });
    }
}







