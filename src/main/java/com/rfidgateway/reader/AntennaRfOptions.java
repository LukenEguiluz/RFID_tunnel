package com.rfidgateway.reader;

import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Valores de TX/RX admitidos por el lector (tablas Octane) o lista genérica si no hay conexión.
 */
@Value
public class AntennaRfOptions {
    List<Double> txPowersDbm;
    List<Double> rxSensitivitiesDbm;
    boolean fromConnectedReader;

    public static AntennaRfOptions fallback() {
        return new AntennaRfOptions(buildDefaultTx(), buildDefaultRx(), false);
    }

    private static List<Double> buildDefaultTx() {
        List<Double> out = new ArrayList<>();
        for (double x = 10.0; x <= 33.0 + 1e-9; x += 0.25) {
            out.add(Math.round(x * 100) / 100.0);
        }
        return Collections.unmodifiableList(out);
    }

    /** Pasos típicos de sensibilidad RX en dBm (orden de más estricto a más permisivo). */
    private static List<Double> buildDefaultRx() {
        List<Double> out = new ArrayList<>();
        for (double x = -80; x >= -130; x -= 1) {
            out.add(x);
        }
        return Collections.unmodifiableList(out);
    }
}
