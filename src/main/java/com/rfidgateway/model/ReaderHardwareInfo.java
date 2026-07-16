package com.rfidgateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Capacidades leídas del hardware (solo marcas con driver conectado).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderHardwareInfo {
    private String readerId;
    private String brand;
    private int antennaCount;
    private String modelName;
    private String modelNumber;
    private String firmwareVersion;
    private boolean xArray;
}
