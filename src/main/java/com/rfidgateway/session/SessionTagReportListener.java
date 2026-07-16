package com.rfidgateway.session;

import com.impinj.octane.ImpinjReader;
import com.impinj.octane.Tag;
import com.impinj.octane.TagReport;
import com.impinj.octane.TagReportListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SessionTagReportListener implements TagReportListener {
    
    @Autowired
    private SessionService sessionService;
    
    private String readerId;
    
    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }
    
    @Override
    public void onTagReported(ImpinjReader reader, TagReport report) {
        if (readerId == null) {
            return;
        }
        
        // Verificar si hay sesión activa para este lector
        if (!sessionService.hasActiveSession(readerId)) {
            return;
        }
        
        // Procesar cada tag reportado
        for (Tag tag : report.getTags()) {
            String epc = tag.getEpc().toHexString();
            
            // Agregar EPC a la sesión activa
            sessionService.addEpcToSession(readerId, epc);
            
            log.debug("EPC {} agregado a sesión activa del lector {}", epc, readerId);
        }
    }
}

