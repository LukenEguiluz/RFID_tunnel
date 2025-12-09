import com.impinj.octane.*;

import java.util.Scanner;

/**
 * Ejemplo simple para verificar que un lector está leyendo tags
 * Uso: java TestLecturaSimple <IP_DEL_LECTOR>
 */
public class TestLecturaSimple {
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java TestLecturaSimple <IP_DEL_LECTOR>");
            System.out.println("Ejemplo: java TestLecturaSimple 192.168.1.100");
            return;
        }
        
        String hostname = args[0];
        ImpinjReader reader = new ImpinjReader();
        
        try {
            System.out.println("=========================================");
            System.out.println("Test de Lectura Simple - SDK Octane");
            System.out.println("=========================================");
            System.out.println("Conectando a lector: " + hostname);
            
            // Conectar
            reader.connect(hostname);
            System.out.println("[OK] Conectado exitosamente");
            
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
            
            // Configurar antenas - habilitar antena 1
            AntennaConfigGroup antennas = settings.getAntennas();
            antennas.disableAll();
            antennas.enableById(new short[]{1});
            
            // Configurar potencia máxima
            AntennaConfig antenna1 = antennas.getAntenna((short) 1);
            antenna1.setIsMaxTxPower(true);
            antenna1.setIsMaxRxSensitivity(true);
            
            System.out.println("[OK] Configuracion aplicada");
            System.out.println("  - Modo: AutoSetDenseReader");
            System.out.println("  - Antena habilitada: 1");
            System.out.println("  - Potencia: Máxima");
            System.out.println("  - Sensibilidad: Máxima");
            
            // Configurar listener para recibir tags
            reader.setTagReportListener(new TagReportListener() {
                @Override
                public void onTagReported(ImpinjReader reader, TagReport report) {
                    for (Tag tag : report.getTags()) {
                        String epc = tag.getEpc().toHexString();
                        short antenna = tag.getAntennaPortNumber();
                        double rssi = tag.getPeakRssiInDbm();
                        
                        System.out.println(String.format(
                            "[TAG] TAG DETECTADO - EPC: %s | Antena: %d | RSSI: %.1f dBm",
                            epc, antenna, rssi
                        ));
                    }
                }
            });
            
            // Aplicar configuración
            System.out.println("\nAplicando configuración...");
            reader.applySettings(settings);
            
            // Iniciar lectura
            System.out.println("Iniciando lectura de tags...");
            System.out.println("Presiona ENTER para detener\n");
            reader.start();
            
            // Esperar hasta que el usuario presione ENTER
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
            
            // Detener y desconectar
            System.out.println("\nDeteniendo lectura...");
            reader.stop();
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


