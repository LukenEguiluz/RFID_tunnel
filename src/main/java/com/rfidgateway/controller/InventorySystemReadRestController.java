/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.rfidgateway.model.Reader
 *  com.rfidgateway.model.ReaderOperationMode
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.rfidgateway.controller;

import com.rfidgateway.inventory.InventoryEpcService;
import com.rfidgateway.model.EpcPresenceEvent;
import com.rfidgateway.model.InventorySystemEpcState;
import com.rfidgateway.model.InventorySystemReader;
import com.rfidgateway.model.Reader;
import com.rfidgateway.model.ReaderOperationMode;
import com.rfidgateway.repository.EpcPresenceEventRepository;
import com.rfidgateway.repository.InventorySystemEpcStateRepository;
import com.rfidgateway.repository.InventorySystemReaderRepository;
import com.rfidgateway.repository.InventorySystemRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/inventory-systems"})
public class InventorySystemReadRestController {
    private static final Logger log = LoggerFactory.getLogger(InventorySystemReadRestController.class);
    @Autowired
    private InventorySystemRepository inventorySystemRepository;
    @Autowired
    private InventorySystemEpcStateRepository epcStateRepository;
    @Autowired
    private EpcPresenceEventRepository presenceEventRepository;
    @Autowired
    private InventorySystemReaderRepository memberRepository;
    @Autowired
    private InventoryEpcService inventoryEpcService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listSystems() {
        List list = this.inventorySystemRepository.findAll().stream().map(s -> {
            HashMap<String, Object> m = new HashMap<String, Object>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("globalCycleSeconds", s.getGlobalCycleSeconds());
            m.put("enabled", s.getEnabled());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping(value={"/{id}"})
    public ResponseEntity<Map<String, Object>> getSystem(@PathVariable UUID id) {
        return this.inventorySystemRepository.findById(id).map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("globalCycleSeconds", s.getGlobalCycleSeconds());
            m.put("cyclesToLost", s.getCyclesToLost());
            m.put("enabled", s.getEnabled());
            return ResponseEntity.ok(m);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value={"/{id}/live"})
    public ResponseEntity<Map<String, Object>> liveContinuous(@PathVariable UUID id) {
        return this.inventorySystemRepository.findById(id).map(system -> {
            Map<String, Object> m = new HashMap<>();
            m.put("systemId", system.getId());
            m.put("name", system.getName());
            m.put("globalCycleSeconds", system.getGlobalCycleSeconds());
            m.put("cyclesToLost", system.getCyclesToLost());
            m.put("enabled", system.getEnabled());
            m.put("cyclesPaused", this.inventoryEpcService.isCyclesPaused(id));
            m.put("cycleActive", this.inventoryEpcService.isCycleActive(id));
            m.put("uniqueEpcsThisCycle", this.inventoryEpcService.getSeenThisCycleCount(id));
            m.put("presentEpcCount", this.epcStateRepository.countBySystemIdAndPresentTrue(id));
            ArrayList readers = new ArrayList();
            for (InventorySystemReader member : this.memberRepository.findBySystem_IdOrderByOrderIndexAsc(id)) {
                Reader r = member.getReader();
                if (r == null) continue;
                HashMap<String, Object> row = new HashMap<String, Object>();
                row.put("readerId", r.getId());
                row.put("name", r.getName());
                row.put("hostname", r.getHostname());
                row.put("connected", Boolean.TRUE.equals(r.getIsConnected()));
                row.put("reading", Boolean.TRUE.equals(r.getIsReading()));
                row.put("readerEnabled", Boolean.TRUE.equals(r.getEnabled()));
                ReaderOperationMode mode = r.getOperationMode();
                row.put("operationMode", mode != null ? mode.name() : ReaderOperationMode.TUNNEL.name());
                row.put("inventorySystemId", r.getInventorySystemId());
                row.put("orderIndex", member.getOrderIndex());
                row.put("readerSlotSeconds", member.getReaderSlotSeconds());
                boolean synced = mode == ReaderOperationMode.CONTINUOUS && id.equals(r.getInventorySystemId());
                row.put("continuousSynced", synced);
                readers.add(row);
            }
            m.put("readers", readers);
            m.put("links", Map.of("epcsCurrent", "/api/inventory-systems/" + id + "/epcs/current", "events", "/api/inventory-systems/" + id + "/events", "websocket", "/ws/events"));
            return ResponseEntity.ok(m);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value={"/{id}/epcs/current"})
    public ResponseEntity<List<InventorySystemEpcState>> currentEpcs(@PathVariable UUID id) {
        if (!this.inventorySystemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(this.epcStateRepository.findBySystemIdAndPresentTrueOrderByLastSeenAtDesc(id));
    }

    @GetMapping(value={"/{id}/epcs/all"})
    public ResponseEntity<Page<InventorySystemEpcState>> allEpcs(@PathVariable UUID id, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        if (!this.inventorySystemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Page<InventorySystemEpcState> p = this.epcStateRepository.findBySystemIdOrderByLastSeenAtDesc(id, (Pageable)PageRequest.of((int)page, (int)Math.min(size, 200), (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"lastSeenAt"})));
        return ResponseEntity.ok(p);
    }

    @GetMapping(value={"/{id}/events"})
    public ResponseEntity<Page<EpcPresenceEvent>> events(@PathVariable UUID id, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="100") int size) {
        if (!this.inventorySystemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Page<EpcPresenceEvent> p = this.presenceEventRepository.findBySystemIdOrderByOccurredAtDesc(id, (Pageable)PageRequest.of((int)page, (int)Math.min(size, 500), (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"occurredAt"})));
        return ResponseEntity.ok(p);
    }

    @GetMapping(value={"/{id}/epcs/{epc}/timeline"})
    public ResponseEntity<Map<String, Object>> epcTimeline(@PathVariable UUID id, @PathVariable String epc) {
        if (!this.inventorySystemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        String norm = epc == null ? "" : epc.trim().toUpperCase();
        HashMap<String, Object> out = new HashMap<String, Object>();
        out.put("systemId", id);
        out.put("epc", norm);
        this.epcStateRepository.findBySystemIdAndEpc(id, norm).ifPresentOrElse(st -> out.put("state", st), () -> out.put("state", null));
        out.put("events", this.presenceEventRepository.findBySystemIdAndEpcOrderByOccurredAtAsc(id, norm));
        return ResponseEntity.ok(out);
    }
}
