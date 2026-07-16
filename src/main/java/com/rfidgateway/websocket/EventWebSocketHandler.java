/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 *  org.springframework.web.socket.CloseStatus
 *  org.springframework.web.socket.TextMessage
 *  org.springframework.web.socket.WebSocketMessage
 *  org.springframework.web.socket.WebSocketSession
 *  org.springframework.web.socket.handler.TextWebSocketHandler
 */
package com.rfidgateway.websocket;

import com.rfidgateway.inventory.RssiProximity;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class EventWebSocketHandler
extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(EventWebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<String, WebSocketSession>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.sessions.put(session.getId(), session);
        log.info("Nueva conexi\u00f3n WebSocket: {}", (Object)session.getId());
    }

    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        this.sessions.remove(session.getId());
        log.info("Conexi\u00f3n WebSocket cerrada: {}", (Object)session.getId());
    }

    public void sendTagDetectedEvent(String readerId, String epc, String antennaId, Short antennaPort, Double rssi, Double phase) {
        Map<String, Object> event = Map.of("type", "TAG_DETECTED", "timestamp", Instant.now().toString(), "data", Map.of("epc", epc, "readerId", readerId, "antennaId", antennaId, "antennaPort", antennaPort != null ? antennaPort : (short)0, "rssi", rssi != null ? rssi : 0.0, "phase", phase != null ? phase : 0.0));
        this.sendToAll(event);
    }

    public void sendReaderDisconnectedEvent(String readerId, String readerName) {
        Map<String, Object> event = Map.of("type", "READER_DISCONNECTED", "timestamp", Instant.now().toString(), "data", Map.of("readerId", readerId, "readerName", readerName != null ? readerName : readerId, "reason", "Connection lost"));
        this.sendToAll(event);
    }

    public void sendReaderReconnectedEvent(String readerId, String readerName) {
        Map<String, Object> event = Map.of("type", "READER_RECONNECTED", "timestamp", Instant.now().toString(), "data", Map.of("readerId", readerId, "readerName", readerName != null ? readerName : readerId));
        this.sendToAll(event);
    }

    public void sendInventoryCycleStart(String systemId) {
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("systemId", systemId);
        HashMap<String, Object> event = new HashMap<String, Object>();
        event.put("type", "INVENTORY_CYCLE_START");
        event.put("timestamp", Instant.now().toString());
        event.put("data", data);
        this.sendToAll(event);
    }

    public void sendInventoryEpcAdd(String systemId, String epc, String readerId, Short antennaPort, Double rssi, Double phase) {
        RssiProximity.Zone zone = RssiProximity.classify(rssi);
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("systemId", systemId);
        data.put("epc", epc);
        data.put("readerId", readerId);
        data.put("antennaPort", antennaPort != null ? antennaPort : (short)0);
        data.put("rssi", rssi);
        data.put("proximity", zone.name());
        data.put("proximityLabel", RssiProximity.labelEs(zone));
        data.put("phase", phase != null ? phase : 0.0);
        HashMap<String, Object> event = new HashMap<String, Object>();
        event.put("type", "INVENTORY_EPC_ADD");
        event.put("timestamp", Instant.now().toString());
        event.put("data", data);
        this.sendToAll(event);
    }

    public void sendInventoryListUpdate(Map<String, Object> envelope) {
        this.sendToAll(envelope);
    }

    public void sendInventoryEpcRemove(String systemId, String epc) {
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("systemId", systemId);
        data.put("epc", epc);
        HashMap<String, Object> event = new HashMap<String, Object>();
        event.put("type", "INVENTORY_EPC_REMOVE");
        event.put("timestamp", Instant.now().toString());
        event.put("data", data);
        this.sendToAll(event);
    }

    private void sendToAll(Map<String, Object> event) {
        String message;
        try {
            message = this.objectMapper.writeValueAsString(event);
        }
        catch (Exception e) {
            log.error("Error al serializar evento: {}", (Object)e.getMessage());
            return;
        }
        TextMessage textMessage = new TextMessage((CharSequence)message);
        this.sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage((WebSocketMessage)textMessage);
                }
            }
            catch (IOException e) {
                log.error("Error al enviar mensaje WebSocket: {}", (Object)e.getMessage());
            }
        });
    }
}
