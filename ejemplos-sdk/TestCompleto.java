import com.impinj.octane.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Test completo con estadísticas detalladas y información de cada tag
 * Uso: java TestCompleto <IP_DEL_LECTOR> [segundos]
 */
public class TestCompleto {
    
    private static class TagInfo {
        String epc;
        short antenna;
        double rssi;
        long seenCount;
        LocalDateTime firstSeen;
        LocalDateTime lastSeen;
        
        TagInfo(String epc, short antenna, double rssi) {
            this.epc = epc;
            this.antenna = antenna;
            this.rssi = rssi;
            this.seenCount = 1;
            this.firstSeen = LocalDateTime.now();
            this.lastSeen = LocalDateTime.now();
        }
        
        void update(double rssi) {
            this.seenCount++;
            this.lastSeen = LocalDateTime.now();
            // Actualizar RSSI si es mayor (mejor señal)
            if (rssi > this.rssi) {
                this.rssi = rssi;
            }
        }
    }
    
    private static Map<String, TagInfo> tags = new HashMap<>();
    private static Map<Short, Integer> tagsPorAntena = new HashMap<>();
    private static int totalReports = 0;
    private static LocalDateTime startTime;
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java TestCompleto <IP_DEL_LECTOR> [segundos]");
            System.out.println("Ejemplo: java TestCompleto 192.168.1.100 30");
            System.out.println("  (lee durante 30 segundos, o presiona ENTER para detener)");
            return;
        }
        
        String hostname = args[0];
        int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        ImpinjReader reader = new ImpinjReader();
        
        try {
            System.out.println("=========================================");
            System.out.println("Test Completo - SDK Octane");
            System.out.println("=========================================");
            System.out.println("Conectando a lector: " + hostname);
            
            // Conectar
            reader.connect(hostname);
            System.out.println("[OK] Conectado exitosamente");
            
            // Obtener información del lector
            FeatureSet features = reader.queryFeatureSet();
            System.out.println("\n[INFO] Informacion del Lector:");
            System.out.println("  - Modelo: " + features.getModelName());
            System.out.println("  - Número de serie: " + features.getSerialNumber());
            System.out.println("  - Firmware: " + features.getFirmwareVersion());
            System.out.println("  - Antenas disponibles: " + features.getAntennaCount());
            
            // Obtener configuración actual
            Settings settings = reader.queryDefaultSettings();
            
            // Configurar modo de lectura
            settings.setReaderMode(ReaderMode.AutoSetDenseReader);
            settings.setSearchMode(SearchMode.SingleTarget);
            settings.setSession((short) 1);
            
            // Configurar reporte completo
            ReportConfig report = settings.getReport();
            report.setMode(ReportMode.Individual);
            report.setIncludeAntennaPortNumber(true);
            report.setIncludePeakRssi(true);
            report.setIncludeLastSeenTime(true);
            report.setIncludeFirstSeenTime(true);
            report.setIncludeSeenCount(true);
            
            // Habilitar todas las antenas
            AntennaConfigGroup antennas = settings.getAntennas();
            antennas.disableAll();
            
            int numAntennas = (int) features.getAntennaCount();
            short[] antennaPorts = new short[numAntennas];
            for (int i = 0; i < numAntennas; i++) {
                antennaPorts[i] = (short)(i + 1);
                tagsPorAntena.put((short)(i + 1), 0);
                
                AntennaConfig antennaConfig = antennas.getAntenna((short)(i + 1));
                antennaConfig.setIsMaxTxPower(true);
                antennaConfig.setIsMaxRxSensitivity(true);
            }
            
            antennas.enableById(antennaPorts);
            
            System.out.println("\n[OK] Configuracion aplicada:");
            System.out.println("  - Modo: AutoSetDenseReader");
            System.out.println("  - Antenas habilitadas: " + numAntennas);
            System.out.println("  - Potencia: Máxima en todas las antenas");
            
            // Configurar listener
            reader.setTagReportListener(new TagReportListener() {
                @Override
                public void onTagReported(ImpinjReader reader, TagReport report) {
                    totalReports++;
                    for (Tag tag : report.getTags()) {
                        String epc = tag.getEpc().toHexString();
                        short antenna = tag.getAntennaPortNumber();
                        double rssi = tag.getPeakRssiInDbm();
                        long seenCount = tag.getTagSeenCount();
                        
                        if (tags.containsKey(epc)) {
                            TagInfo info = tags.get(epc);
                            info.update(rssi);
                        } else {
                            TagInfo info = new TagInfo(epc, antenna, rssi);
                            tags.put(epc, info);
                            tagsPorAntena.put(antenna, tagsPorAntena.get(antenna) + 1);
                            
                            System.out.println(String.format(
                                "[NEW] NUEVO TAG - EPC: %s | Antena: %d | RSSI: %.1f dBm",
                                epc, antenna, rssi
                            ));
                        }
                    }
                }
            });
            
            // Aplicar configuración
            System.out.println("\nAplicando configuración...");
            reader.applySettings(settings);
            
            // Iniciar lectura
            startTime = LocalDateTime.now();
            System.out.println("Iniciando lectura de tags...");
            System.out.println("Hora de inicio: " + startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            
            if (seconds > 0) {
                System.out.println("Leyendo durante " + seconds + " segundos...\n");
            } else {
                System.out.println("Presiona ENTER para detener y ver estadísticas\n");
            }
            
            reader.start();
            
            // Esperar tiempo especificado o hasta ENTER
            if (seconds > 0) {
                Thread.sleep(seconds * 1000);
            } else {
                Scanner scanner = new Scanner(System.in);
                scanner.nextLine();
            }
            
            // Detener
            System.out.println("\nDeteniendo lectura...");
            reader.stop();
            
            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            
            // Mostrar estadísticas completas
            System.out.println("\n=========================================");
            System.out.println("[STATS] ESTADISTICAS COMPLETAS");
            System.out.println("=========================================");
            System.out.println("Tiempo de lectura: " + durationSeconds + " segundos");
            System.out.println("Total de reportes recibidos: " + totalReports);
            System.out.println("Tags únicos detectados: " + tags.size());
            System.out.println("\nTags por antena:");
            for (Map.Entry<Short, Integer> entry : tagsPorAntena.entrySet()) {
                System.out.println(String.format(
                    "  Antena %d: %d tags únicos",
                    entry.getKey(), entry.getValue()
                ));
            }
            
            System.out.println("\n[DETAIL] Detalle de Tags:");
            System.out.println("----------------------------------------");
            List<TagInfo> sortedTags = new ArrayList<>(tags.values());
            sortedTags.sort((a, b) -> Long.compare(b.seenCount, a.seenCount));
            
            for (TagInfo tag : sortedTags) {
                System.out.println(String.format(
                    "EPC: %s\n" +
                    "  Antena: %d\n" +
                    "  RSSI máximo: %.1f dBm\n" +
                    "  Veces detectado: %d\n" +
                    "  Primera vez: %s\n" +
                    "  Última vez: %s\n",
                    tag.epc,
                    tag.antenna,
                    tag.rssi,
                    tag.seenCount,
                    tag.firstSeen.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    tag.lastSeen.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
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


