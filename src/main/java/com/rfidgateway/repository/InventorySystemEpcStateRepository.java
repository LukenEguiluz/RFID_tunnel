/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.rfidgateway.repository;

import com.rfidgateway.model.EpcPresenceState;
import com.rfidgateway.model.InventorySystemEpcState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventorySystemEpcStateRepository
extends JpaRepository<InventorySystemEpcState, Long> {
    public List<InventorySystemEpcState> findBySystemIdAndPresentTrueOrderByLastSeenAtDesc(UUID var1);

    public Page<InventorySystemEpcState> findBySystemIdOrderByLastSeenAtDesc(UUID var1, Pageable var2);

    public Optional<InventorySystemEpcState> findBySystemIdAndEpc(UUID var1, String var2);

    public List<InventorySystemEpcState> findBySystemIdAndPresentTrue(UUID var1);

    public List<InventorySystemEpcState> findBySystemIdAndPresenceState(UUID systemId, EpcPresenceState presenceState);

    public long countBySystemIdAndPresentTrue(UUID var1);

    public void deleteBySystemId(UUID var1);
}
