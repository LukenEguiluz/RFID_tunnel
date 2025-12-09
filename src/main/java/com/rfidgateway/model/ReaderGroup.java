package com.rfidgateway.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "reader_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReaderGroup {
    
    @Id
    private String id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "group_readers",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "reader_id")
    )
    private List<Reader> readers = new ArrayList<>();
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Obtiene solo los lectores habilitados del grupo
     */
    public List<Reader> getEnabledReaders() {
        return readers.stream()
            .filter(reader -> reader.getEnabled() != null && reader.getEnabled())
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene solo los lectores conectados del grupo
     */
    public List<Reader> getConnectedReaders() {
        return readers.stream()
            .filter(reader -> reader.getIsConnected() != null && reader.getIsConnected())
            .collect(Collectors.toList());
    }
}

