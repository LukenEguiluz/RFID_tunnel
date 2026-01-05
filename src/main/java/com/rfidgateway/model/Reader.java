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
    
    // Configuración de potencia de transmisión
    @Column(name = "default_tx_power_dbm")
    private Double defaultTxPowerDbm; // Potencia por defecto en dBm para todas las antenas (null = usar potencia máxima)
    
    @Column(name = "max_tx_power_dbm")
    private Double maxTxPowerDbm; // Potencia máxima permitida en dBm (null = sin límite)
    
    @Column(name = "use_default_power")
    private Boolean useDefaultPower = false; // Si true, todas las antenas usan defaultTxPowerDbm (ignora potencia individual)
    
    // Configuración de sensibilidad de recepción
    @Column(name = "default_rx_sensitivity_dbm")
    private Double defaultRxSensitivityDbm; // Sensibilidad por defecto en dBm para todas las antenas (null = usar sensibilidad máxima)
    
    // Tiempo entre cambios de antena (en milisegundos)
    @Column(name = "antenna_switch_delay_ms")
    private Integer antennaSwitchDelayMs = 100; // Tiempo de espera al cambiar entre antenas (por defecto 100ms)
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}



