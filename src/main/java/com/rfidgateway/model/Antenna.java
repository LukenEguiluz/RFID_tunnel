package com.rfidgateway.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "antennas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Antenna {
    
    @Id
    private String id;
    
    @Column(name = "reader_id", nullable = false)
    private String readerId;
    
    @Column
    private String name;
    
    @Column(name = "port_number", nullable = false)
    private Short portNumber;
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "tx_power_dbm")
    private Double txPowerDbm;
    
    @Column(name = "rx_sensitivity_dbm")
    private Double rxSensitivityDbm;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}






