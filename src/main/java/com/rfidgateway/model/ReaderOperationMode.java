package com.rfidgateway.model;

/**
 * TUNNEL: lectura bajo demanda (inicio manual / sesiones).
 * CONTINUOUS: inventario cíclico gestionado por {@link com.rfidgateway.inventory.InventoryOrchestrationService}.
 */
public enum ReaderOperationMode {
    TUNNEL,
    CONTINUOUS
}
