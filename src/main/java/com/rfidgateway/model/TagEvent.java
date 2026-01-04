package com.rfidgateway.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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





