package com.rfidgateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory_systems")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventorySystem {

    public static final String DEFAULT_LIST_WEBHOOK_EVENT_ID = "evt_00000000800000000000000000000001";

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "global_cycle_seconds", nullable = false)
    private Integer globalCycleSeconds = 300;

    @Column(nullable = false)
    private Boolean enabled = false;

    /** Ciclos consecutivos sin lectura antes de removed en inventario/webhook (default 3). */
    @Column(name = "cycles_to_lost", nullable = false)
    private Integer cyclesToLost = 3;

    @Column(name = "cycles_to_remove_after_lost", nullable = false)
    private Integer cyclesToRemoveAfterLost = 3;

    @Column(name = "inventory_list_webhook_enabled", nullable = false)
    private Boolean inventoryListWebhookEnabled = false;

    @Column(name = "inventory_list_webhook_url", columnDefinition = "TEXT")
    private String inventoryListWebhookUrl;

    @Column(name = "inventory_list_webhook_secret", length = 256)
    private String inventoryListWebhookSecret;

    @Column(name = "inventory_list_webhook_event_id", length = 64)
    private String inventoryListWebhookEventId = DEFAULT_LIST_WEBHOOK_EVENT_ID;

    @OneToMany(mappedBy = "system", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<InventorySystemReader> members = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.inventoryListWebhookEventId == null || this.inventoryListWebhookEventId.isBlank()) {
            this.inventoryListWebhookEventId = DEFAULT_LIST_WEBHOOK_EVENT_ID;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
