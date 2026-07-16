package com.rfidgateway.inventory;

import com.rfidgateway.model.EpcPresenceEvent;
import com.rfidgateway.model.EpcPresenceEventType;
import com.rfidgateway.model.EpcPresenceState;
import com.rfidgateway.model.InventorySystem;
import com.rfidgateway.model.InventorySystemEpcState;
import com.rfidgateway.repository.EpcPresenceEventRepository;
import com.rfidgateway.repository.InventorySystemEpcStateRepository;
import com.rfidgateway.repository.InventorySystemRepository;
import com.rfidgateway.websocket.EventWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class InventoryEpcService {

    /** Ciclos consecutivos sin lectura → sale del inventario (webhook removed). */
    public static final int DEFAULT_CYCLES_TO_REMOVED = 3;

    private static final String MANUAL_DEMO_READER = "manual-demo";

    private final ConcurrentHashMap<UUID, Boolean> cycleActive = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> cyclesPaused = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<String>> seenThisCycle = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<String>> pendingListAdds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<String>> pendingListRemoves = new ConcurrentHashMap<>();

    @Autowired
    private InventorySystemEpcStateRepository stateRepository;

    @Autowired
    private InventorySystemRepository inventorySystemRepository;

    @Autowired
    private EpcPresenceEventRepository eventRepository;

    @Autowired(required = false)
    private EventWebSocketHandler webSocketHandler;

    @Autowired(required = false)
    private InventoryListWebhookService inventoryListWebhookService;

    public void beginCycle(UUID systemId) {
        seenThisCycle.put(systemId, ConcurrentHashMap.newKeySet());
        cycleActive.put(systemId, true);
        if (webSocketHandler != null) {
            webSocketHandler.sendInventoryCycleStart(systemId.toString());
        }
    }

    @Transactional
    public void finishCycle(UUID systemId) {
        Set<String> seen = seenThisCycle.get(systemId);
        if (seen == null) {
            seen = Collections.emptySet();
        }

        InventorySystem system = inventorySystemRepository.findById(systemId).orElse(null);
        int cyclesToRemoved = resolveCyclesToRemoved(system);

        List<InventorySystemEpcState> presentRows = stateRepository.findBySystemIdAndPresentTrue(systemId);

        // Ciclo sin ninguna lectura: no penalizar (fallo de lectores, reinicio, etc.)
        if (seen.isEmpty() && !presentRows.isEmpty()) {
            log.warn(
                "Ciclo sin lecturas — sistema {} ({} EPCs presentes); se omite evaluación de presencia",
                systemId,
                presentRows.size()
            );
            cycleActive.put(systemId, false);
            seenThisCycle.remove(systemId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (InventorySystemEpcState row : presentRows) {
            if (seen.contains(row.getEpc())) {
                if (safeInt(row.getMissedCycles()) > 0) {
                    row.setMissedCycles(0);
                    stateRepository.save(row);
                }
                continue;
            }

            int missed = safeInt(row.getMissedCycles()) + 1;
            row.setMissedCycles(missed);

            if (missed < cyclesToRemoved) {
                stateRepository.save(row);
                log.debug(
                    "EPC {} no leído en ciclo ({}/{}) — sigue en inventario — sistema {}",
                    row.getEpc(),
                    missed,
                    cyclesToRemoved,
                    systemId
                );
                continue;
            }

            row.setPresent(false);
            row.setPresenceState(EpcPresenceState.REMOVED);
            row.setLostCycles(0);
            stateRepository.save(row);
            trackListRemove(systemId, row.getEpc());
            savePresenceEvent(systemId, row.getEpc(), EpcPresenceEventType.REMOVE, now, null, null);
            if (webSocketHandler != null) {
                webSocketHandler.sendInventoryEpcRemove(systemId.toString(), row.getEpc());
            }
            log.info(
                "EPC {} → removed ({} ciclos sin lectura) — sistema {}",
                row.getEpc(),
                missed,
                systemId
            );
        }

        // Etiquetas LOST heredadas: pasan a REMOVED si siguen sin leerse
        for (InventorySystemEpcState row : stateRepository.findBySystemIdAndPresenceState(systemId, EpcPresenceState.LOST)) {
            if (seen.contains(row.getEpc())) {
                continue;
            }
            row.setPresenceState(EpcPresenceState.REMOVED);
            stateRepository.save(row);
            log.info("EPC {} → REMOVED (estado LOST heredado) — sistema {}", row.getEpc(), systemId);
        }

        flushInventoryListWebhook(systemId);
        cycleActive.put(systemId, false);
        seenThisCycle.remove(systemId);
    }

    @Transactional
    public void recordTag(
        UUID systemId,
        String readerId,
        String epcRaw,
        Short antennaPort,
        Double rssi,
        Double phase
    ) {
        if (!Boolean.TRUE.equals(cycleActive.get(systemId))) {
            return;
        }
        String epc = normalizeEpc(epcRaw);
        if (epc.isEmpty()) {
            return;
        }
        Set<String> seen = seenThisCycle.get(systemId);
        if (seen == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean firstInCycle = seen.add(epc);

        if (!firstInCycle) {
            stateRepository.findBySystemIdAndEpc(systemId, epc).ifPresent(s -> {
                s.setLastSeenAt(now);
                s.setPresent(true);
                s.setLastReaderId(readerId);
                s.setLastAntennaPort(antennaPort);
                if (rssi != null) {
                    s.setLastRssi(rssi);
                }
                stateRepository.save(s);
            });
            return;
        }

        Optional<InventorySystemEpcState> opt = stateRepository.findBySystemIdAndEpc(systemId, epc);
        boolean becamePresent;

        if (opt.isEmpty()) {
            becamePresent = true;
            InventorySystemEpcState s = new InventorySystemEpcState();
            s.setSystemId(systemId);
            s.setEpc(epc);
            s.setFirstSeenAt(now);
            s.setLastSeenAt(now);
            s.setPresent(true);
            s.setPresenceState(EpcPresenceState.PRESENT);
            s.setMissedCycles(0);
            s.setLostCycles(0);
            s.setLastRssi(rssi);
            s.setLastReaderId(readerId);
            s.setLastAntennaPort(antennaPort);
            stateRepository.save(s);
            savePresenceEvent(systemId, epc, EpcPresenceEventType.ADD, now, readerId, antennaPort);
            if (webSocketHandler != null) {
                webSocketHandler.sendInventoryEpcAdd(systemId.toString(), epc, readerId, antennaPort, rssi, phase);
            }
        } else {
            InventorySystemEpcState s = opt.get();
            becamePresent = !Boolean.TRUE.equals(s.getPresent());
            s.setLastSeenAt(now);
            s.setPresent(true);
            s.setPresenceState(EpcPresenceState.PRESENT);
            s.setMissedCycles(0);
            s.setLostCycles(0);
            s.setLastRssi(rssi);
            s.setLastReaderId(readerId);
            s.setLastAntennaPort(antennaPort);
            stateRepository.save(s);
            if (becamePresent) {
                savePresenceEvent(systemId, epc, EpcPresenceEventType.ADD, now, readerId, antennaPort);
                if (webSocketHandler != null) {
                    webSocketHandler.sendInventoryEpcAdd(systemId.toString(), epc, readerId, antennaPort, rssi, phase);
                }
            }
        }

        if (becamePresent) {
            trackListAdd(systemId, epc);
        }
    }

    private void savePresenceEvent(
        UUID systemId,
        String epc,
        EpcPresenceEventType type,
        LocalDateTime occurredAt,
        String readerId,
        Short antennaPort
    ) {
        EpcPresenceEvent ev = new EpcPresenceEvent();
        ev.setSystemId(systemId);
        ev.setEpc(epc);
        ev.setEventType(type);
        ev.setOccurredAt(occurredAt);
        ev.setReaderId(readerId);
        ev.setAntennaPort(antennaPort);
        eventRepository.save(ev);
    }

    private int resolveCyclesToRemoved(InventorySystem system) {
        if (system == null || system.getCyclesToLost() == null || system.getCyclesToLost() < 1) {
            return DEFAULT_CYCLES_TO_REMOVED;
        }
        return system.getCyclesToLost();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private void trackListAdd(UUID systemId, String epc) {
        pendingListAdds.computeIfAbsent(systemId, k -> ConcurrentHashMap.newKeySet()).add(epc);
        pendingListRemoves.computeIfAbsent(systemId, k -> ConcurrentHashMap.newKeySet()).remove(epc);
    }

    private void trackListRemove(UUID systemId, String epc) {
        pendingListRemoves.computeIfAbsent(systemId, k -> ConcurrentHashMap.newKeySet()).add(epc);
        pendingListAdds.computeIfAbsent(systemId, k -> ConcurrentHashMap.newKeySet()).remove(epc);
    }

    private void flushInventoryListWebhook(UUID systemId) {
        if (inventoryListWebhookService == null) {
            pendingListAdds.remove(systemId);
            pendingListRemoves.remove(systemId);
            return;
        }
        Set<String> added = pendingListAdds.remove(systemId);
        Set<String> removed = pendingListRemoves.remove(systemId);
        if (added == null) {
            added = Collections.emptySet();
        }
        if (removed == null) {
            removed = Collections.emptySet();
        }
        if (!added.isEmpty() || !removed.isEmpty()) {
            inventoryListWebhookService.publishListUpdate(systemId, added, removed, false, false);
        }
    }

    private static String normalizeEpc(String epc) {
        return epc == null ? "" : epc.trim().toUpperCase();
    }

    public boolean isCyclesPaused(UUID systemId) {
        return Boolean.TRUE.equals(cyclesPaused.get(systemId));
    }

    public void setCyclesPaused(UUID systemId, boolean paused) {
        if (paused) {
            cyclesPaused.put(systemId, true);
            log.info("Ciclos pausados (demo) — sistema {}", systemId);
        } else {
            cyclesPaused.remove(systemId);
            log.info("Ciclos reanudados — sistema {}", systemId);
        }
    }

    /** Marca salida manual (mock): removed + webhook/WS. */
    @Transactional
    public void manualMarkRemoved(UUID systemId, String epcRaw) {
        String epc = normalizeEpc(epcRaw);
        if (epc.isEmpty()) {
            throw new IllegalArgumentException("Indicá un EPC");
        }
        InventorySystemEpcState row = stateRepository.findBySystemIdAndEpc(systemId, epc)
            .orElseThrow(() -> new IllegalArgumentException("EPC no encontrado en este sistema"));
        if (!Boolean.TRUE.equals(row.getPresent())) {
            throw new IllegalArgumentException("EPC ya no está en inventario");
        }
        LocalDateTime now = LocalDateTime.now();
        row.setPresent(false);
        row.setPresenceState(EpcPresenceState.REMOVED);
        row.setMissedCycles(0);
        row.setLostCycles(0);
        stateRepository.save(row);
        savePresenceEvent(systemId, epc, EpcPresenceEventType.REMOVE, now, MANUAL_DEMO_READER, null);
        if (webSocketHandler != null) {
            webSocketHandler.sendInventoryEpcRemove(systemId.toString(), epc);
        }
        publishManualDelta(systemId, List.of(), List.of(epc));
    }

    /** Marca regreso manual (mock): added + webhook/WS. */
    @Transactional
    public void manualMarkReturned(UUID systemId, String epcRaw) {
        String epc = normalizeEpc(epcRaw);
        if (epc.isEmpty()) {
            throw new IllegalArgumentException("Indicá un EPC");
        }
        LocalDateTime now = LocalDateTime.now();
        Optional<InventorySystemEpcState> opt = stateRepository.findBySystemIdAndEpc(systemId, epc);
        if (opt.isEmpty()) {
            InventorySystemEpcState s = new InventorySystemEpcState();
            s.setSystemId(systemId);
            s.setEpc(epc);
            s.setFirstSeenAt(now);
            s.setLastSeenAt(now);
            s.setPresent(true);
            s.setPresenceState(EpcPresenceState.PRESENT);
            s.setMissedCycles(0);
            s.setLostCycles(0);
            s.setLastReaderId(MANUAL_DEMO_READER);
            s.setLastAntennaPort((short) 0);
            stateRepository.save(s);
        } else {
            InventorySystemEpcState s = opt.get();
            if (Boolean.TRUE.equals(s.getPresent())) {
                throw new IllegalArgumentException("EPC ya está en inventario");
            }
            s.setLastSeenAt(now);
            s.setPresent(true);
            s.setPresenceState(EpcPresenceState.PRESENT);
            s.setMissedCycles(0);
            s.setLostCycles(0);
            s.setLastReaderId(MANUAL_DEMO_READER);
            s.setLastAntennaPort((short) 0);
            stateRepository.save(s);
        }
        savePresenceEvent(systemId, epc, EpcPresenceEventType.ADD, now, MANUAL_DEMO_READER, null);
        if (webSocketHandler != null) {
            webSocketHandler.sendInventoryEpcAdd(systemId.toString(), epc, MANUAL_DEMO_READER, (short) 0, null, null);
        }
        publishManualDelta(systemId, List.of(epc), List.of());
    }

    private void publishManualDelta(UUID systemId, List<String> added, List<String> removed) {
        if (inventoryListWebhookService != null) {
            inventoryListWebhookService.publishListUpdate(systemId, added, removed, false, true);
        }
    }

    public boolean isCycleActive(UUID systemId) {
        return Boolean.TRUE.equals(cycleActive.get(systemId));
    }

    public int getSeenThisCycleCount(UUID systemId) {
        Set<String> s = seenThisCycle.get(systemId);
        return s == null ? 0 : s.size();
    }
}
