/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Modifying
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 *  org.springframework.stereotype.Repository
 */
package com.rfidgateway.repository;

import com.rfidgateway.model.InventorySystemReader;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventorySystemReaderRepository
extends JpaRepository<InventorySystemReader, Long> {
    public List<InventorySystemReader> findBySystem_IdOrderByOrderIndexAsc(UUID var1);

    @Modifying(flushAutomatically=true, clearAutomatically=true)
    @Query(value="DELETE FROM InventorySystemReader m WHERE m.system.id = :systemId")
    public void deleteBySystem_Id(@Param(value="systemId") UUID var1);

    public Optional<InventorySystemReader> findByReader_Id(String var1);
}
