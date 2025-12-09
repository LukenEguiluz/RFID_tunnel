package com.rfidgateway.reader;

import com.impinj.octane.ConnectionLostListener;
import com.impinj.octane.ImpinjReader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GatewayConnectionLostListener implements ConnectionLostListener {
    
    private final String readerId;
    private final ReaderManager readerManager;
    
    public GatewayConnectionLostListener(String readerId, ReaderManager readerManager) {
        this.readerId = readerId;
        this.readerManager = readerManager;
    }
    
    @Override
    public void onConnectionLost(ImpinjReader reader) {
        log.warn("Conexión perdida con lector {}", readerId);
        readerManager.handleConnectionLost(readerId);
    }
}



