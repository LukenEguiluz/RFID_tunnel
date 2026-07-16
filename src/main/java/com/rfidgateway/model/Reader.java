/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.rfidgateway.model.ReaderBrand
 *  com.rfidgateway.model.ReaderOperationMode
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.EnumType
 *  javax.persistence.Enumerated
 *  javax.persistence.Id
 *  javax.persistence.PrePersist
 *  javax.persistence.PreUpdate
 *  javax.persistence.Table
 */
package com.rfidgateway.model;

import com.rfidgateway.model.ReaderBrand;
import com.rfidgateway.model.ReaderOperationMode;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name="readers")
public class Reader {
    @Id
    private String id;
    @Column(nullable=false, unique=true)
    private String name;
    @Column(nullable=false)
    private String hostname;
    @Column(nullable=false)
    private Boolean enabled = true;
    @Enumerated(value=EnumType.STRING)
    @Column(name="operation_mode", nullable=false, length=32)
    private ReaderOperationMode operationMode = ReaderOperationMode.TUNNEL;
    @Enumerated(value=EnumType.STRING)
    @Column(name="brand", nullable=false, length=32)
    private ReaderBrand brand = ReaderBrand.IMPINJ_OCTANE;
    @Column(name="inventory_system_id", columnDefinition="uuid")
    private UUID inventorySystemId;
    @Column(name="is_connected")
    private Boolean isConnected = false;
    @Column(name="is_reading")
    private Boolean isReading = false;
    @Column(name="last_seen")
    private LocalDateTime lastSeen;
    @Column(name="created_at", updatable=false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name="updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersistReader() {
        if (this.operationMode == null) {
            this.operationMode = ReaderOperationMode.TUNNEL;
        }
        if (this.brand == null) {
            this.brand = ReaderBrand.IMPINJ_OCTANE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.brand == null) {
            this.brand = ReaderBrand.IMPINJ_OCTANE;
        }
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getHostname() {
        return this.hostname;
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public ReaderOperationMode getOperationMode() {
        return this.operationMode;
    }

    public ReaderBrand getBrand() {
        return this.brand;
    }

    public UUID getInventorySystemId() {
        return this.inventorySystemId;
    }

    public Boolean getIsConnected() {
        return this.isConnected;
    }

    public Boolean getIsReading() {
        return this.isReading;
    }

    public LocalDateTime getLastSeen() {
        return this.lastSeen;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setOperationMode(ReaderOperationMode operationMode) {
        this.operationMode = operationMode;
    }

    public void setBrand(ReaderBrand brand) {
        this.brand = brand;
    }

    public void setInventorySystemId(UUID inventorySystemId) {
        this.inventorySystemId = inventorySystemId;
    }

    public void setIsConnected(Boolean isConnected) {
        this.isConnected = isConnected;
    }

    public void setIsReading(Boolean isReading) {
        this.isReading = isReading;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Reader)) {
            return false;
        }
        Reader other = (Reader)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$enabled = this.getEnabled();
        Boolean other$enabled = other.getEnabled();
        if (this$enabled == null ? other$enabled != null : !((Object)this$enabled).equals(other$enabled)) {
            return false;
        }
        Boolean this$isConnected = this.getIsConnected();
        Boolean other$isConnected = other.getIsConnected();
        if (this$isConnected == null ? other$isConnected != null : !((Object)this$isConnected).equals(other$isConnected)) {
            return false;
        }
        Boolean this$isReading = this.getIsReading();
        Boolean other$isReading = other.getIsReading();
        if (this$isReading == null ? other$isReading != null : !((Object)this$isReading).equals(other$isReading)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$hostname = this.getHostname();
        String other$hostname = other.getHostname();
        if (this$hostname == null ? other$hostname != null : !this$hostname.equals(other$hostname)) {
            return false;
        }
        ReaderOperationMode this$operationMode = this.getOperationMode();
        ReaderOperationMode other$operationMode = other.getOperationMode();
        if (this$operationMode == null ? other$operationMode != null : !this$operationMode.equals(other$operationMode)) {
            return false;
        }
        ReaderBrand this$brand = this.getBrand();
        ReaderBrand other$brand = other.getBrand();
        if (this$brand == null ? other$brand != null : !this$brand.equals(other$brand)) {
            return false;
        }
        UUID this$inventorySystemId = this.getInventorySystemId();
        UUID other$inventorySystemId = other.getInventorySystemId();
        if (this$inventorySystemId == null ? other$inventorySystemId != null : !((Object)this$inventorySystemId).equals(other$inventorySystemId)) {
            return false;
        }
        LocalDateTime this$lastSeen = this.getLastSeen();
        LocalDateTime other$lastSeen = other.getLastSeen();
        if (this$lastSeen == null ? other$lastSeen != null : !((Object)this$lastSeen).equals(other$lastSeen)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt)) {
            return false;
        }
        LocalDateTime this$updatedAt = this.getUpdatedAt();
        LocalDateTime other$updatedAt = other.getUpdatedAt();
        return !(this$updatedAt == null ? other$updatedAt != null : !((Object)this$updatedAt).equals(other$updatedAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Reader;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $enabled = this.getEnabled();
        result = result * 59 + ($enabled == null ? 43 : ((Object)$enabled).hashCode());
        Boolean $isConnected = this.getIsConnected();
        result = result * 59 + ($isConnected == null ? 43 : ((Object)$isConnected).hashCode());
        Boolean $isReading = this.getIsReading();
        result = result * 59 + ($isReading == null ? 43 : ((Object)$isReading).hashCode());
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $hostname = this.getHostname();
        result = result * 59 + ($hostname == null ? 43 : $hostname.hashCode());
        ReaderOperationMode $operationMode = this.getOperationMode();
        result = result * 59 + ($operationMode == null ? 43 : $operationMode.hashCode());
        ReaderBrand $brand = this.getBrand();
        result = result * 59 + ($brand == null ? 43 : $brand.hashCode());
        UUID $inventorySystemId = this.getInventorySystemId();
        result = result * 59 + ($inventorySystemId == null ? 43 : ((Object)$inventorySystemId).hashCode());
        LocalDateTime $lastSeen = this.getLastSeen();
        result = result * 59 + ($lastSeen == null ? 43 : ((Object)$lastSeen).hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "Reader(id=" + this.getId() + ", name=" + this.getName() + ", hostname=" + this.getHostname() + ", enabled=" + this.getEnabled() + ", operationMode=" + this.getOperationMode() + ", brand=" + this.getBrand() + ", inventorySystemId=" + this.getInventorySystemId() + ", isConnected=" + this.getIsConnected() + ", isReading=" + this.getIsReading() + ", lastSeen=" + this.getLastSeen() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }

    public Reader() {
    }

    public Reader(String id, String name, String hostname, Boolean enabled, ReaderOperationMode operationMode, ReaderBrand brand, UUID inventorySystemId, Boolean isConnected, Boolean isReading, LocalDateTime lastSeen, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.hostname = hostname;
        this.enabled = enabled;
        this.operationMode = operationMode;
        this.brand = brand;
        this.inventorySystemId = inventorySystemId;
        this.isConnected = isConnected;
        this.isReading = isReading;
        this.lastSeen = lastSeen;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
