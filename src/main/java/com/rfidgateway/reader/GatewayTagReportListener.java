package com.rfidgateway.reader;

import com.impinj.octane.ImpinjReader;
import com.impinj.octane.Tag;
import com.impinj.octane.TagReport;
import com.impinj.octane.TagReportListener;
import com.rfidgateway.tag.TagEventService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class GatewayTagReportListener implements TagReportListener {
    
    private final String readerId;
    private final TagEventService tagEventService;
    
    public GatewayTagReportListener(String readerId, TagEventService tagEventService) {
        this.readerId = readerId;
        this.tagEventService = tagEventService;
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
                
                // Procesar evento de tag
                tagEventService.processTagEvent(
                    readerId,
                    epc,
                    antennaPort,
                    rssi,
                    phase
                );
                
            } catch (Exception e) {
                log.error("Error al procesar tag del lector {}: {}", readerId, e.getMessage(), e);
            }
        }
    }
}


