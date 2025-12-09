package com.rfidgateway.reader;

import com.impinj.octane.ImpinjReader;
import com.impinj.octane.Tag;
import com.impinj.octane.TagReport;
import com.impinj.octane.TagReportListener;
import com.rfidgateway.tag.TagEventService;
import com.rfidgateway.session.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
public class GatewayTagReportListener implements TagReportListener {
    
    private final String readerId;
    private final TagEventService tagEventService;
    private SessionService sessionService;
    
    public GatewayTagReportListener(String readerId, TagEventService tagEventService) {
        this.readerId = readerId;
        this.tagEventService = tagEventService;
    }
    
    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }
    
    @Override
    public void onTagReported(ImpinjReader reader, TagReport report) {
        List<Tag> tags = report.getTags();
        
        log.info("onTagReported llamado - Lector: {}, Tags en reporte: {}", readerId, tags.size());
        
        if (tags.isEmpty()) {
            log.debug("Reporte vacío recibido del lector {}", readerId);
            return;
        }
        
        log.info("Lector {} reportó {} tag(s)", readerId, tags.size());
        
        for (Tag tag : tags) {
            try {
                String epc = tag.getEpc().toHexString();
                Short antennaPort = tag.isAntennaPortNumberPresent() 
                    ? tag.getAntennaPortNumber() 
                    : null;
                Double rssi = tag.isPeakRssiInDbmPresent() 
                    ? tag.getPeakRssiInDbm() 
                    : null;
                Double phase = tag.isRfPhaseAnglePresent() 
                    ? tag.getPhaseAngleInRadians() 
                    : null;
                
                log.info("TAG DETECTADO - Lector: {}, EPC: {}, Antena: {}, RSSI: {} dBm", 
                        readerId, epc, antennaPort, rssi);
                
                // Procesar evento de tag (siempre guardar en BD)
                tagEventService.processTagEvent(
                    readerId,
                    epc,
                    antennaPort,
                    rssi,
                    phase
                );
                
                // Si hay sesión activa, también agregar a la sesión
                if (sessionService != null && sessionService.hasActiveSession(readerId)) {
                    sessionService.addEpcToSession(readerId, epc);
                    log.debug("EPC {} agregado a sesión activa del lector {}", epc, readerId);
                }
                
            } catch (Exception e) {
                log.error("Error al procesar tag del lector {}: {}", readerId, e.getMessage(), e);
            }
        }
    }
}


