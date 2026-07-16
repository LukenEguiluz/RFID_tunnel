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

import com.rfidgateway.model.EpcPresenceEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EpcPresenceEventRepository
extends JpaRepository<EpcPresenceEvent, UUID> {
    public Page<EpcPresenceEvent> findBySystemIdOrderByOccurredAtDesc(UUID var1, Pageable var2);

    public List<EpcPresenceEvent> findBySystemIdAndEpcOrderByOccurredAtAsc(UUID var1, String var2);

    public void deleteBySystemId(UUID var1);
}
