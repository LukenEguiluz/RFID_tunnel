import com.impinj.octane.*;

/**
 * Test simple para verificar solo la conexión al lector
 * Uso: java TestConexion <IP_DEL_LECTOR>
 */
public class TestConexion {
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java TestConexion <IP_DEL_LECTOR>");
            System.out.println("Ejemplo: java TestConexion 192.168.1.100");
            return;
        }
        
        String hostname = args[0];
        ImpinjReader reader = new ImpinjReader();
        
        try {
            System.out.println("=========================================");
            System.out.println("Test de Conexión - SDK Octane");
            System.out.println("=========================================");
            System.out.println("Conectando a: " + hostname);
            
            // Intentar conectar
            reader.connect(hostname);
            System.out.println("[OK] Conexion exitosa!");
            
            // Obtener información del lector
            FeatureSet features = reader.queryFeatureSet();
            
            System.out.println("\n[INFO] Informacion del Lector:");
            System.out.println("  [OK] Modelo: " + features.getModelName());
            System.out.println("  [OK] Numero de serie: " + features.getSerialNumber());
            System.out.println("  [OK] Firmware: " + features.getFirmwareVersion());
            System.out.println("  [OK] Antenas disponibles: " + features.getAntennaCount());
            System.out.println("  [OK] GPIs disponibles: " + features.getGpiCount());
            System.out.println("  [OK] GPOs disponibles: " + features.getGpoCount());
            
            // Obtener configuración actual
            Settings settings = reader.querySettings();
            System.out.println("\n[CONFIG] Configuracion Actual:");
            System.out.println("  - Modo de lector: " + settings.getReaderMode());
            System.out.println("  - Modo de busqueda: " + settings.getSearchMode());
            System.out.println("  - Sesion: " + settings.getSession());
            
            // Verificar estado de antenas
            AntennaConfigGroup antennas = settings.getAntennas();
            System.out.println("\n[ANTENNA] Estado de Antenas:");
            for (int i = 1; i <= features.getAntennaCount(); i++) {
                AntennaConfig antenna = antennas.getAntenna((short) i);
                String estado = antenna.isEnabled() ? "[OK] Habilitada" : "[X] Deshabilitada";
                System.out.println("  Antena " + i + ": " + estado);
                
                if (antenna.isEnabled()) {
                    if (antenna.getIsMaxTxPower()) {
                        System.out.println("    - Potencia: Maxima");
                    } else {
                        System.out.println("    - Potencia: " + antenna.getTxPowerinDbm() + " dBm");
                    }
                    
                    if (antenna.getIsMaxRxSensitivity()) {
                        System.out.println("    - Sensibilidad: Maxima");
                    } else {
                        System.out.println("    - Sensibilidad: " + antenna.getRxSensitivityinDbm() + " dBm");
                    }
                }
            }
            
            // Verificar estado del lector
            Status status = reader.queryStatus();
            System.out.println("\n[STATUS] Estado del Lector:");
            System.out.println("  - Conectado: " + (reader.isConnected() ? "Si" : "No"));
            System.out.println("  - Leyendo: " + (status.getIsConnected() ? "Si" : "No"));
            System.out.println("  - Temperatura: " + status.getTemperatureCelsius() + "C");
            
            // Desconectar
            reader.disconnect();
            System.out.println("\n[OK] Desconectado correctamente");
            System.out.println("\n[OK] Test completado exitosamente!");
            
        } catch (OctaneSdkException e) {
            System.err.println("\n[ERROR] Error del SDK: " + e.getMessage());
            if (e.getMessage().contains("connect")) {
                System.err.println("\nPosibles causas:");
                System.err.println("  - La IP del lector es incorrecta");
                System.err.println("  - El lector no está encendido");
                System.err.println("  - Problemas de red/firewall");
                System.err.println("  - El lector está siendo usado por otra aplicación");
            }
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("\n[ERROR] Error: " + e.getMessage());
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


