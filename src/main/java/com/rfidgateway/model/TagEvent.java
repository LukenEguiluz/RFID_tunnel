package com.rfidgateway.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tag_events", indexes = {
    @Index(name = "idx_epc", columnList = "epc"),
    @Index(name = "idx_reader", columnList = "reader_id"),
    @Index(name = "idx_antenna", columnList = "antenna_id"),
    @Index(name = "idx_detected_at", columnList = "detected_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagEvent {

    @Id
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @PrePersist
    protected void assignId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
    
    @Column(nullable = false, length = 96)
    private String epc;
    
    @Column(name = "reader_id", nullable = false)
    private String readerId;
    
    @Column(name = "antenna_id", nullable = false)
    private String antennaId;
    
    @Column(name = "antenna_port", nullable = false)
    private Short antennaPort;
    
    @Column
    private Double rssi;
    
    @Column
    private Double phase;
    
    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt = LocalDateTime.now();
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}







