package com.rfidgateway.tag;

import com.rfidgateway.websocket.EventWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WebSocketEventService {
    
    @Autowired(required = false)
    private EventWebSocketHandler webSocketHandler;
    
    public void notifyTagDetected(String readerId, String epc, String antennaId, 
                                  Short antennaPort, Double rssi, Double phase) {
        if (webSocketHandler != null) {
            webSocketHandler.sendTagDetectedEvent(readerId, epc, antennaId, antennaPort, rssi, phase);
        }
    }
    
    public void notifyReaderDisconnected(String readerId, String readerName) {
        if (webSocketHandler != null) {
            webSocketHandler.sendReaderDisconnectedEvent(readerId, readerName);
        }
    }
    
    public void notifyReaderReconnected(String readerId, String readerName) {
        if (webSocketHandler != null) {
            webSocketHandler.sendReaderReconnectedEvent(readerId, readerName);
        }
    }
}





