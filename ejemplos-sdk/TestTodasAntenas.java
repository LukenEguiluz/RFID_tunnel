import com.impinj.octane.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Ejemplo para probar todas las antenas del lector
 * Uso: java TestTodasAntenas <IP_DEL_LECTOR>
 */
public class TestTodasAntenas {
    
    private static Map<Short, Integer> tagsPorAntena = new HashMap<>();
    private static int totalTags = 0;
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java TestTodasAntenas <IP_DEL_LECTOR>");
            System.out.println("Ejemplo: java TestTodasAntenas 192.168.1.100");
            return;
        }
        
        String hostname = args[0];
        ImpinjReader reader = new ImpinjReader();
        
        try {
            System.out.println("=========================================");
            System.out.println("Test de Todas las Antenas - SDK Octane");
            System.out.println("=========================================");
            System.out.println("Conectando a lector: " + hostname);
            
            // Conectar
            reader.connect(hostname);
            System.out.println("[OK] Conectado exitosamente");
            
            // Obtener información del lector
            FeatureSet features = reader.queryFeatureSet();
            System.out.println("\n[INFO] Informacion del Lector:");
            System.out.println("  - Modelo: " + features.getModelName());
            System.out.println("  - Firmware: " + features.getFirmwareVersion());
            System.out.println("  - Número de antenas: " + features.getAntennaCount());
            
            // Obtener configuración por defecto
            Settings settings = reader.queryDefaultSettings();
            
            // Configurar modo de lectura
            settings.setReaderMode(ReaderMode.AutoSetDenseReader);
            settings.setSearchMode(SearchMode.SingleTarget);
            settings.setSession((short) 1);
            
            // Configurar reporte
            ReportConfig report = settings.getReport();
            report.setMode(ReportMode.Individual);
            report.setIncludeAntennaPortNumber(true);
            report.setIncludePeakRssi(true);
            report.setIncludeLastSeenTime(true);
            report.setIncludeSeenCount(true);
            
            // Habilitar todas las antenas disponibles
            AntennaConfigGroup antennas = settings.getAntennas();
            antennas.disableAll();
            
            int numAntennas = (int) features.getAntennaCount();
            short[] antennaPorts = new short[numAntennas];
            for (int i = 0; i < numAntennas; i++) {
                antennaPorts[i] = (short)(i + 1);
                tagsPorAntena.put((short)(i + 1), 0);
                
                // Configurar cada antena con potencia máxima
                AntennaConfig antennaConfig = antennas.getAntenna((short)(i + 1));
                antennaConfig.setIsMaxTxPower(true);
                antennaConfig.setIsMaxRxSensitivity(true);
            }
            
            antennas.enableById(antennaPorts);
            
            System.out.println("\n[OK] Configuracion aplicada:");
            System.out.println("  - Antenas habilitadas: " + numAntennas);
            for (int i = 1; i <= numAntennas; i++) {
                System.out.println("    • Antena " + i + " (Potencia: Máxima)");
            }
            
            // Configurar listener
            reader.setTagReportListener(new TagReportListener() {
                @Override
                public void onTagReported(ImpinjReader reader, TagReport report) {
                    for (Tag tag : report.getTags()) {
                        String epc = tag.getEpc().toHexString();
                        short antenna = tag.getAntennaPortNumber();
                        double rssi = tag.getPeakRssiInDbm();
                        long seenCount = tag.getTagSeenCount();
                        
                        totalTags++;
                        tagsPorAntena.put(antenna, tagsPorAntena.get(antenna) + 1);
                        
                        System.out.println(String.format(
                            "[TAG] [Antena %d] EPC: %s | RSSI: %.1f dBm | Veces visto: %d",
                            antenna, epc, rssi, seenCount
                        ));
                    }
                }
            });
            
            // Aplicar configuración
            System.out.println("\nAplicando configuración...");
            reader.applySettings(settings);
            
            // Iniciar lectura
            System.out.println("Iniciando lectura de tags...");
            System.out.println("Presiona ENTER para detener y ver estadísticas\n");
            reader.start();
            
            // Esperar hasta que el usuario presione ENTER
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
            
            // Detener
            System.out.println("\nDeteniendo lectura...");
            reader.stop();
            
            // Mostrar estadísticas
            System.out.println("\n=========================================");
            System.out.println("[STATS] ESTADISTICAS");
            System.out.println("=========================================");
            System.out.println("Total de tags detectados: " + totalTags);
            System.out.println("\nTags por antena:");
            for (Map.Entry<Short, Integer> entry : tagsPorAntena.entrySet()) {
                System.out.println(String.format(
                    "  Antena %d: %d tags (%.1f%%)",
                    entry.getKey(),
                    entry.getValue(),
                    totalTags > 0 ? (entry.getValue() * 100.0 / totalTags) : 0
                ));
            }
            System.out.println("=========================================\n");
            
            // Desconectar
            reader.disconnect();
            System.out.println("[OK] Desconectado");
            
        } catch (OctaneSdkException e) {
            System.err.println("[ERROR] Error del SDK: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[ERROR] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (reader.isConnected()) {
                    reader.disconnect();
                }
            } catch (Exception e) {
                // Ignorar errores al desconectar
            }
        }
    }
}


