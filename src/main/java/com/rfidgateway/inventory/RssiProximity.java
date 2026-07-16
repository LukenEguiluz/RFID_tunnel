package com.rfidgateway.inventory;

import com.rfidgateway.model.InventorySystemEpcState;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estima cercanía de una etiqueta a partir del RSSI (dBm).
 * Valores menos negativos = señal más fuerte = más cerca del lector.
 */
public final class RssiProximity {

    /** RSSI &gt;= este valor → CERCA (señal fuerte). */
    public static final double NEAR_RSSI_DBM = -55.0;
    /** RSSI &gt;= este valor (y &lt; NEAR) → MEDIA. Por debajo → LEJOS. */
    public static final double MEDIUM_RSSI_DBM = -70.0;

    public enum Zone {
        CERCA,
        MEDIA,
        LEJOS,
        DESCONOCIDA
    }

    private RssiProximity() {
    }

    public static Zone classify(Double rssi) {
        if (rssi == null) {
            return Zone.DESCONOCIDA;
        }
        if (rssi >= NEAR_RSSI_DBM) {
            return Zone.CERCA;
        }
        if (rssi >= MEDIUM_RSSI_DBM) {
            return Zone.MEDIA;
        }
        return Zone.LEJOS;
    }

    public static String labelEs(Zone zone) {
        if (zone == null) {
            return "—";
        }
        switch (zone) {
            case CERCA:
                return "Cerca";
            case MEDIA:
                return "Media";
            case LEJOS:
                return "Lejos";
            default:
                return "—";
        }
    }

    public static Map<String, Object> toTagDetail(InventorySystemEpcState row) {
        Zone zone = classify(row.getLastRssi());
        Map<String, Object> tag = new LinkedHashMap<>();
        tag.put("epc", row.getEpc());
        tag.put("rssi", row.getLastRssi());
        tag.put("proximity", zone.name());
        tag.put("proximityLabel", labelEs(zone));
        if (row.getLastReaderId() != null) {
            tag.put("readerId", row.getLastReaderId());
        }
        if (row.getLastAntennaPort() != null) {
            tag.put("antennaPort", row.getLastAntennaPort());
        }
        tag.put("missedCycles", row.getMissedCycles() != null ? row.getMissedCycles() : 0);
        return tag;
    }
}
