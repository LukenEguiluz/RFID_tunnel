package com.rfidgateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(
    name = "inventory_system_readers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"system_id", "reader_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventorySystemReader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id", nullable = false)
    private InventorySystem system;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "reader_id", nullable = false)
    private Reader reader;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    /**
     * Segundos asignados a este lector dentro de cada ciclo global (se reparten entre sus antenas habilitadas).
     */
    @Column(name = "reader_slot_seconds", nullable = false)
    private Integer readerSlotSeconds = 60;
}
