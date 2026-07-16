/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.EnumType
 *  javax.persistence.Enumerated
 *  javax.persistence.Id
 *  javax.persistence.Index
 *  javax.persistence.PrePersist
 *  javax.persistence.Table
 */
package com.rfidgateway.model;

import com.rfidgateway.model.EpcPresenceEventType;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name="epc_presence_events", indexes={@Index(name="idx_epc_presence_system_time", columnList="system_id,occurred_at")})
public class EpcPresenceEvent {
    @Id
    @Column(name="id", columnDefinition="uuid", updatable=false, nullable=false)
    private UUID id;
    @Column(name="system_id", nullable=false, columnDefinition="uuid")
    private UUID systemId;
    @Column(nullable=false, length=128)
    private String epc;
    @Enumerated(value=EnumType.STRING)
    @Column(name="event_type", nullable=false, length=16)
    private EpcPresenceEventType eventType;
    @Column(name="occurred_at", nullable=false)
    private LocalDateTime occurredAt;
    @Column(name="reader_id", length=64)
    private String readerId;
    @Column(name="antenna_port")
    private Short antennaPort;

    @PrePersist
    protected void assignId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return this.id;
    }

    public UUID getSystemId() {
        return this.systemId;
    }

    public String getEpc() {
        return this.epc;
    }

    public EpcPresenceEventType getEventType() {
        return this.eventType;
    }

    public LocalDateTime getOccurredAt() {
        return this.occurredAt;
    }

    public String getReaderId() {
        return this.readerId;
    }

    public Short getAntennaPort() {
        return this.antennaPort;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setSystemId(UUID systemId) {
        this.systemId = systemId;
    }

    public void setEpc(String epc) {
        this.epc = epc;
    }

    public void setEventType(EpcPresenceEventType eventType) {
        this.eventType = eventType;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }

    public void setAntennaPort(Short antennaPort) {
        this.antennaPort = antennaPort;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EpcPresenceEvent)) {
            return false;
        }
        EpcPresenceEvent other = (EpcPresenceEvent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Short this$antennaPort = this.getAntennaPort();
        Short other$antennaPort = other.getAntennaPort();
        if (this$antennaPort == null ? other$antennaPort != null : !((Object)this$antennaPort).equals(other$antennaPort)) {
            return false;
        }
        UUID this$id = this.getId();
        UUID other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        UUID this$systemId = this.getSystemId();
        UUID other$systemId = other.getSystemId();
        if (this$systemId == null ? other$systemId != null : !((Object)this$systemId).equals(other$systemId)) {
            return false;
        }
        String this$epc = this.getEpc();
        String other$epc = other.getEpc();
        if (this$epc == null ? other$epc != null : !this$epc.equals(other$epc)) {
            return false;
        }
        EpcPresenceEventType this$eventType = this.getEventType();
        EpcPresenceEventType other$eventType = other.getEventType();
        if (this$eventType == null ? other$eventType != null : !((Object)((Object)this$eventType)).equals((Object)other$eventType)) {
            return false;
        }
        LocalDateTime this$occurredAt = this.getOccurredAt();
        LocalDateTime other$occurredAt = other.getOccurredAt();
        if (this$occurredAt == null ? other$occurredAt != null : !((Object)this$occurredAt).equals(other$occurredAt)) {
            return false;
        }
        String this$readerId = this.getReaderId();
        String other$readerId = other.getReaderId();
        return !(this$readerId == null ? other$readerId != null : !this$readerId.equals(other$readerId));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EpcPresenceEvent;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Short $antennaPort = this.getAntennaPort();
        result = result * 59 + ($antennaPort == null ? 43 : ((Object)$antennaPort).hashCode());
        UUID $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        UUID $systemId = this.getSystemId();
        result = result * 59 + ($systemId == null ? 43 : ((Object)$systemId).hashCode());
        String $epc = this.getEpc();
        result = result * 59 + ($epc == null ? 43 : $epc.hashCode());
        EpcPresenceEventType $eventType = this.getEventType();
        result = result * 59 + ($eventType == null ? 43 : ((Object)((Object)$eventType)).hashCode());
        LocalDateTime $occurredAt = this.getOccurredAt();
        result = result * 59 + ($occurredAt == null ? 43 : ((Object)$occurredAt).hashCode());
        String $readerId = this.getReaderId();
        result = result * 59 + ($readerId == null ? 43 : $readerId.hashCode());
        return result;
    }

    public String toString() {
        return "EpcPresenceEvent(id=" + this.getId() + ", systemId=" + this.getSystemId() + ", epc=" + this.getEpc() + ", eventType=" + this.getEventType() + ", occurredAt=" + this.getOccurredAt() + ", readerId=" + this.getReaderId() + ", antennaPort=" + this.getAntennaPort() + ")";
    }

    public EpcPresenceEvent() {
    }

    public EpcPresenceEvent(UUID id, UUID systemId, String epc, EpcPresenceEventType eventType, LocalDateTime occurredAt, String readerId, Short antennaPort) {
        this.id = id;
        this.systemId = systemId;
        this.epc = epc;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.readerId = readerId;
        this.antennaPort = antennaPort;
    }
}
