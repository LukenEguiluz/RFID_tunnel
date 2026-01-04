package com.rfidgateway.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "readers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reader {
    
    @Id
    private String id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(nullable = false)
    private String hostname;
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "is_connected")
    private Boolean isConnected = false;
    
    @Column(name = "is_reading")
    private Boolean isReading = false;
    
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Configuración de lectura intermitente
    @Column(name = "intermittent_enabled")
    private Boolean intermittentEnabled = false;
    
    @Column(name = "read_duration_seconds")
    private Integer readDurationSeconds = 5; // Duración de lectura en segundos
    
    @Column(name = "pause_duration_seconds")
    private Integer pauseDurationSeconds = 5; // Duración de pausa en segundos
    
    @Column(name = "connected_antennas_count")
    private Integer connectedAntennasCount = 0; // Cantidad de antenas conectadas al lector
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}



