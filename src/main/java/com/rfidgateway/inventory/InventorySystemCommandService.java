/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.rfidgateway.model.Reader
 *  com.rfidgateway.model.ReaderOperationMode
 *  com.rfidgateway.repository.ReaderRepository
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.rfidgateway.inventory;

import com.rfidgateway.model.InventorySystem;
import com.rfidgateway.model.InventorySystemReader;
import com.rfidgateway.model.Reader;
import com.rfidgateway.model.ReaderOperationMode;
import com.rfidgateway.repository.EpcPresenceEventRepository;
import com.rfidgateway.repository.InventorySystemEpcStateRepository;
import com.rfidgateway.repository.InventorySystemReaderRepository;
import com.rfidgateway.repository.InventorySystemRepository;
import com.rfidgateway.repository.ReaderRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventorySystemCommandService {
    @Autowired
    private InventorySystemRepository inventorySystemRepository;
    @Autowired
    private InventorySystemReaderRepository memberRepository;
    @Autowired
    private ReaderRepository readerRepository;
    @Autowired
    private InventorySystemEpcStateRepository epcStateRepository;
    @Autowired
    private EpcPresenceEventRepository presenceEventRepository;

    @Transactional
    public UUID createSystem(String name, int globalCycleSeconds, int cyclesToLost, boolean enabled, List<String> memberReaderId, List<Integer> memberOrder, List<Integer> memberSlotSeconds) {
        InventorySystem s = new InventorySystem();
        s.setName(name != null ? name.trim() : "Sistema");
        s.setGlobalCycleSeconds(Math.max(30, globalCycleSeconds));
        s.setCyclesToLost(Math.max(1, Math.min(20, cyclesToLost)));
        s.setEnabled(enabled);
        this.inventorySystemRepository.save(s);
        this.applyMembers(s.getId(), memberReaderId, memberOrder, memberSlotSeconds);
        return s.getId();
    }

    @Transactional
    public void updateSystem(UUID id, String name, int globalCycleSeconds, int cyclesToLost, boolean enabled, List<String> memberReaderId, List<Integer> memberOrder, List<Integer> memberSlotSeconds) {
        this.inventorySystemRepository.findById(id).ifPresent(s -> {
            s.setName(name != null ? name.trim() : s.getName());
            s.setGlobalCycleSeconds(Math.max(30, globalCycleSeconds));
            s.setCyclesToLost(Math.max(1, Math.min(20, cyclesToLost)));
            s.setEnabled(enabled);
            this.inventorySystemRepository.save(s);
        });
        this.applyMembers(id, memberReaderId, memberOrder, memberSlotSeconds);
    }

    @Transactional
    public void deleteSystem(UUID id) {
        this.memberRepository.findBySystem_IdOrderByOrderIndexAsc(id).forEach(m -> {
            Reader r = m.getReader();
            if (r != null) {
                r.setOperationMode(ReaderOperationMode.TUNNEL);
                r.setInventorySystemId(null);
                this.readerRepository.save(r);
            }
        });
        this.memberRepository.deleteBySystem_Id(id);
        this.epcStateRepository.deleteBySystemId(id);
        this.presenceEventRepository.deleteBySystemId(id);
        this.inventorySystemRepository.deleteById(id);
    }

    private void applyMembers(UUID systemId, List<String> memberReaderId, List<Integer> memberOrder, List<Integer> memberSlotSeconds) {
        this.memberRepository.deleteBySystem_Id(systemId);
        InventorySystem system = (InventorySystem)this.inventorySystemRepository.findById(systemId).orElseThrow();
        LinkedHashSet<String> assigned = new LinkedHashSet<String>();
        if (memberReaderId != null) {
            for (int i = 0; i < memberReaderId.size(); ++i) {
                Reader reader;
                String rid = memberReaderId.get(i);
                if (rid == null || rid.isBlank() || !assigned.add(rid = rid.trim()) || (reader = this.readerRepository.findById(rid).orElse(null)) == null) continue;
                this.memberRepository.findByReader_Id(rid).ifPresent(arg_0 -> ((InventorySystemReaderRepository)this.memberRepository).delete(arg_0));
                int ord = memberOrder != null && i < memberOrder.size() && memberOrder.get(i) != null ? memberOrder.get(i) : i;
                int slot = memberSlotSeconds != null && i < memberSlotSeconds.size() && memberSlotSeconds.get(i) != null ? memberSlotSeconds.get(i) : 60;
                slot = Math.max(5, slot);
                reader.setOperationMode(ReaderOperationMode.CONTINUOUS);
                reader.setInventorySystemId(systemId);
                this.readerRepository.save(reader);
                InventorySystemReader row = new InventorySystemReader();
                row.setSystem(system);
                row.setReader(reader);
                row.setOrderIndex(ord);
                row.setReaderSlotSeconds(slot);
                this.memberRepository.save(row);
            }
        }
        this.readerRepository.findAll().stream().filter(r -> systemId.equals(r.getInventorySystemId()) && !assigned.contains(r.getId())).forEach(r -> {
            r.setOperationMode(ReaderOperationMode.TUNNEL);
            r.setInventorySystemId(null);
            this.readerRepository.save(r);
        });
    }
}
