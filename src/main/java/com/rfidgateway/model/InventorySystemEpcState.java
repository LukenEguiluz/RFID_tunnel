/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.EnumType
 *  javax.persistence.Enumerated
 *  javax.persistence.GeneratedValue
 *  javax.persistence.GenerationType
 *  javax.persistence.Id
 *  javax.persistence.Table
 *  javax.persistence.UniqueConstraint
 */
package com.rfidgateway.model;

import com.rfidgateway.model.EpcPresenceState;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="inventory_system_epc_states", uniqueConstraints={@UniqueConstraint(columnNames={"system_id", "epc"})})
public class InventorySystemEpcState {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="system_id", nullable=false, columnDefinition="uuid")
    private UUID systemId;
    @Column(nullable=false, length=128)
    private String epc;
    @Column(name="first_seen_at", nullable=false)
    private LocalDateTime firstSeenAt;
    @Column(name="last_seen_at", nullable=false)
    private LocalDateTime lastSeenAt;
    @Column(nullable=false)
    private Boolean present = true;
    @Enumerated(value=EnumType.STRING)
    @Column(name="presence_state", nullable=false, length=10)
    private EpcPresenceState presenceState = EpcPresenceState.PRESENT;
    @Column(name="missed_cycles", nullable=false)
    private Integer missedCycles = 0;
    @Column(name="lost_cycles", nullable=false)
    private Integer lostCycles = 0;
    @Column(name="last_rssi")
    private Double lastRssi;
    @Column(name="last_reader_id", length=64)
    private String lastReaderId;
    @Column(name="last_antenna_port")
    private Short lastAntennaPort;

    public Long getId() {
        return this.id;
    }

    public UUID getSystemId() {
        return this.systemId;
    }

    public String getEpc() {
        return this.epc;
    }

    public LocalDateTime getFirstSeenAt() {
        return this.firstSeenAt;
    }

    public LocalDateTime getLastSeenAt() {
        return this.lastSeenAt;
    }

    public Boolean getPresent() {
        return this.present;
    }

    public EpcPresenceState getPresenceState() {
        return this.presenceState;
    }

    public Integer getMissedCycles() {
        return this.missedCycles;
    }

    public Integer getLostCycles() {
        return this.lostCycles;
    }

    public Double getLastRssi() {
        return this.lastRssi;
    }

    public String getLastReaderId() {
        return this.lastReaderId;
    }

    public Short getLastAntennaPort() {
        return this.lastAntennaPort;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSystemId(UUID systemId) {
        this.systemId = systemId;
    }

    public void setEpc(String epc) {
        this.epc = epc;
    }

    public void setFirstSeenAt(LocalDateTime firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public void setPresent(Boolean present) {
        this.present = present;
    }

    public void setPresenceState(EpcPresenceState presenceState) {
        this.presenceState = presenceState;
    }

    public void setMissedCycles(Integer missedCycles) {
        this.missedCycles = missedCycles;
    }

    public void setLostCycles(Integer lostCycles) {
        this.lostCycles = lostCycles;
    }

    public void setLastRssi(Double lastRssi) {
        this.lastRssi = lastRssi;
    }

    public void setLastReaderId(String lastReaderId) {
        this.lastReaderId = lastReaderId;
    }

    public void setLastAntennaPort(Short lastAntennaPort) {
        this.lastAntennaPort = lastAntennaPort;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof InventorySystemEpcState)) {
            return false;
        }
        InventorySystemEpcState other = (InventorySystemEpcState)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Boolean this$present = this.getPresent();
        Boolean other$present = other.getPresent();
        if (this$present == null ? other$present != null : !((Object)this$present).equals(other$present)) {
            return false;
        }
        Integer this$missedCycles = this.getMissedCycles();
        Integer other$missedCycles = other.getMissedCycles();
        if (this$missedCycles == null ? other$missedCycles != null : !((Object)this$missedCycles).equals(other$missedCycles)) {
            return false;
        }
        Integer this$lostCycles = this.getLostCycles();
        Integer other$lostCycles = other.getLostCycles();
        if (this$lostCycles == null ? other$lostCycles != null : !((Object)this$lostCycles).equals(other$lostCycles)) {
            return false;
        }
        Double this$lastRssi = this.getLastRssi();
        Double other$lastRssi = other.getLastRssi();
        if (this$lastRssi == null ? other$lastRssi != null : !((Object)this$lastRssi).equals(other$lastRssi)) {
            return false;
        }
        Short this$lastAntennaPort = this.getLastAntennaPort();
        Short other$lastAntennaPort = other.getLastAntennaPort();
        if (this$lastAntennaPort == null ? other$lastAntennaPort != null : !((Object)this$lastAntennaPort).equals(other$lastAntennaPort)) {
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
        LocalDateTime this$firstSeenAt = this.getFirstSeenAt();
        LocalDateTime other$firstSeenAt = other.getFirstSeenAt();
        if (this$firstSeenAt == null ? other$firstSeenAt != null : !((Object)this$firstSeenAt).equals(other$firstSeenAt)) {
            return false;
        }
        LocalDateTime this$lastSeenAt = this.getLastSeenAt();
        LocalDateTime other$lastSeenAt = other.getLastSeenAt();
        if (this$lastSeenAt == null ? other$lastSeenAt != null : !((Object)this$lastSeenAt).equals(other$lastSeenAt)) {
            return false;
        }
        EpcPresenceState this$presenceState = this.getPresenceState();
        EpcPresenceState other$presenceState = other.getPresenceState();
        if (this$presenceState == null ? other$presenceState != null : !((Object)((Object)this$presenceState)).equals((Object)other$presenceState)) {
            return false;
        }
        String this$lastReaderId = this.getLastReaderId();
        String other$lastReaderId = other.getLastReaderId();
        return !(this$lastReaderId == null ? other$lastReaderId != null : !this$lastReaderId.equals(other$lastReaderId));
    }

    protected boolean canEqual(Object other) {
        return other instanceof InventorySystemEpcState;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Boolean $present = this.getPresent();
        result = result * 59 + ($present == null ? 43 : ((Object)$present).hashCode());
        Integer $missedCycles = this.getMissedCycles();
        result = result * 59 + ($missedCycles == null ? 43 : ((Object)$missedCycles).hashCode());
        Integer $lostCycles = this.getLostCycles();
        result = result * 59 + ($lostCycles == null ? 43 : ((Object)$lostCycles).hashCode());
        Double $lastRssi = this.getLastRssi();
        result = result * 59 + ($lastRssi == null ? 43 : ((Object)$lastRssi).hashCode());
        Short $lastAntennaPort = this.getLastAntennaPort();
        result = result * 59 + ($lastAntennaPort == null ? 43 : ((Object)$lastAntennaPort).hashCode());
        UUID $systemId = this.getSystemId();
        result = result * 59 + ($systemId == null ? 43 : ((Object)$systemId).hashCode());
        String $epc = this.getEpc();
        result = result * 59 + ($epc == null ? 43 : $epc.hashCode());
        LocalDateTime $firstSeenAt = this.getFirstSeenAt();
        result = result * 59 + ($firstSeenAt == null ? 43 : ((Object)$firstSeenAt).hashCode());
        LocalDateTime $lastSeenAt = this.getLastSeenAt();
        result = result * 59 + ($lastSeenAt == null ? 43 : ((Object)$lastSeenAt).hashCode());
        EpcPresenceState $presenceState = this.getPresenceState();
        result = result * 59 + ($presenceState == null ? 43 : ((Object)((Object)$presenceState)).hashCode());
        String $lastReaderId = this.getLastReaderId();
        result = result * 59 + ($lastReaderId == null ? 43 : $lastReaderId.hashCode());
        return result;
    }

    public String toString() {
        return "InventorySystemEpcState(id=" + this.getId() + ", systemId=" + this.getSystemId() + ", epc=" + this.getEpc() + ", firstSeenAt=" + this.getFirstSeenAt() + ", lastSeenAt=" + this.getLastSeenAt() + ", present=" + this.getPresent() + ", presenceState=" + this.getPresenceState() + ", missedCycles=" + this.getMissedCycles() + ", lostCycles=" + this.getLostCycles() + ", lastRssi=" + this.getLastRssi() + ", lastReaderId=" + this.getLastReaderId() + ", lastAntennaPort=" + this.getLastAntennaPort() + ")";
    }

    public InventorySystemEpcState() {
    }

    public InventorySystemEpcState(Long id, UUID systemId, String epc, LocalDateTime firstSeenAt, LocalDateTime lastSeenAt, Boolean present, EpcPresenceState presenceState, Integer missedCycles, Integer lostCycles, Double lastRssi, String lastReaderId, Short lastAntennaPort) {
        this.id = id;
        this.systemId = systemId;
        this.epc = epc;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
        this.present = present;
        this.presenceState = presenceState;
        this.missedCycles = missedCycles;
        this.lostCycles = lostCycles;
        this.lastRssi = lastRssi;
        this.lastReaderId = lastReaderId;
        this.lastAntennaPort = lastAntennaPort;
    }
}
