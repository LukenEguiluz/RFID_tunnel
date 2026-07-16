/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package com.rfidgateway.repository;

import com.rfidgateway.model.InventorySystem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventorySystemRepository
extends JpaRepository<InventorySystem, UUID> {
    public List<InventorySystem> findByEnabledTrue();
}
