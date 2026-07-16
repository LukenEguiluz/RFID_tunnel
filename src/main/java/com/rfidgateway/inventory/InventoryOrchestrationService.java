/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.rfidgateway.model.Antenna
 *  com.rfidgateway.model.Reader
 *  com.rfidgateway.model.ReaderOperationMode
 *  com.rfidgateway.reader.ReaderManager
 *  com.rfidgateway.repository.AntennaRepository
 *  javax.annotation.PostConstruct
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 */
package com.rfidgateway.inventory;

import com.rfidgateway.inventory.InventoryEpcService;
import com.rfidgateway.model.Antenna;
import com.rfidgateway.model.InventorySystem;
import com.rfidgateway.model.InventorySystemReader;
import com.rfidgateway.model.Reader;
import com.rfidgateway.model.ReaderOperationMode;
import com.rfidgateway.reader.ReaderManager;
import com.rfidgateway.repository.AntennaRepository;
import com.rfidgateway.repository.InventorySystemReaderRepository;
import com.rfidgateway.repository.InventorySystemRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InventoryOrchestrationService {
    private static final Logger log = LoggerFactory.getLogger(InventoryOrchestrationService.class);
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> futures = new ConcurrentHashMap();
    @Autowired
    private InventorySystemRepository systemRepository;
    @Autowired
    private InventorySystemReaderRepository memberRepository;
    @Autowired
    private AntennaRepository antennaRepository;
    @Autowired
    private ReaderManager readerManager;
    @Autowired
    private InventoryEpcService inventoryEpcService;

    @PostConstruct
    public void init() {
        this.reload();
    }

    public synchronized void reload() {
        for (ScheduledFuture<?> f : this.futures.values()) {
            f.cancel(false);
        }
        this.futures.clear();
        for (InventorySystem s : this.systemRepository.findAll()) {
            if (!Boolean.TRUE.equals(s.getEnabled())) continue;
            this.startLoop(s.getId());
        }
    }

    private void startLoop(UUID systemId) {
        CycleRunner runner = new CycleRunner(systemId);
        ScheduledFuture<?> f = this.executor.schedule(runner, 5L, TimeUnit.SECONDS);
        this.futures.put(systemId, f);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void runOneCycle(InventorySystem system) throws Exception {
        UUID systemId = system.getId();
        if (this.inventoryEpcService.isCyclesPaused(systemId)) {
            log.debug("Ciclo omitido — pausa demo activa — sistema {}", systemId);
            return;
        }
        this.inventoryEpcService.beginCycle(systemId);
        try {
            List<InventorySystemReader> members = this.memberRepository.findBySystem_IdOrderByOrderIndexAsc(systemId);
            for (InventorySystemReader member : members) {
                Reader reader = member.getReader();
                if (reader == null || reader.getOperationMode() != ReaderOperationMode.CONTINUOUS || !systemId.equals(reader.getInventorySystemId()) || !Boolean.TRUE.equals(reader.getEnabled())) continue;
                this.processReaderSlots(reader.getId(), member.getReaderSlotSeconds());
            }
        }
        finally {
            this.inventoryEpcService.finishCycle(systemId);
        }
    }

    private void processReaderSlots(String readerId, int readerSlotSeconds) throws Exception {
        long totalMs = (long)readerSlotSeconds * 1000L;
        List<Antenna> ants = this.antennaRepository.findByReaderIdAndEnabledTrueOrderByPortNumberAsc(readerId);
        ArrayList<Short> ports = new ArrayList<Short>();
        for (Antenna a : ants) {
            ports.add(a.getPortNumber());
        }
        if (ports.isEmpty()) {
            this.readerManager.runInventoryAntennaSlot(readerId, (short)1, Math.max(200L, totalMs));
            return;
        }
        int n = ports.size();
        long perAntMs = Math.max(200L, totalMs / (long)n);
        for (Short port : ports) {
            this.readerManager.runInventoryAntennaSlot(readerId, port.shortValue(), perAntMs);
        }
    }

    private final class CycleRunner
    implements Runnable {
        private final UUID systemId;

        CycleRunner(UUID systemId) {
            this.systemId = systemId;
        }

        @Override
        public void run() {
            InventorySystem s = InventoryOrchestrationService.this.systemRepository.findById(this.systemId).orElse(null);
            if (s == null || !Boolean.TRUE.equals(s.getEnabled())) {
                InventoryOrchestrationService.this.futures.remove(this.systemId);
                return;
            }
            long t0 = System.currentTimeMillis();
            try {
                InventoryOrchestrationService.this.runOneCycle(s);
            }
            catch (Exception e) {
                log.error("Ciclo inventario continuo fall\u00f3 sistema {}: {}", new Object[]{this.systemId, e.getMessage(), e});
            }
            long elapsed = System.currentTimeMillis() - t0;
            long waitMs = Math.max(0L, (long)s.getGlobalCycleSeconds().intValue() * 1000L - elapsed);
            ScheduledFuture<?> next = InventoryOrchestrationService.this.executor.schedule(this, waitMs, TimeUnit.MILLISECONDS);
            InventoryOrchestrationService.this.futures.put(this.systemId, next);
        }
    }
}
