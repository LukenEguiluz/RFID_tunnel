package com.rfidgateway.model;

/**
 * Marca / stack de control del lector. El gateway delegará comandos según la marca
 * (hoy solo {@link #IMPINJ_OCTANE} está implementado con Octane SDK).
 */
public enum ReaderBrand {
    /** Impinj R420/R700/etc. vía Octane Java SDK (LLRP). */
    IMPINJ_OCTANE,
    /** Reservado para futuros lectores Zebra con otro SDK. */
    ZEBRA_FUTURE,
    /** Otro fabricante — sin conexión SDK hasta implementar el driver. */
    OTHER
}
