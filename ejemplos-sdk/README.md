# 📡 Ejemplos de SDK Octane - Verificación de Lectores

Esta carpeta contiene ejemplos de código usando el SDK de Octane directamente para verificar que los lectores y antenas estén funcionando correctamente.

## 🎯 Propósito

Estos ejemplos te permiten:
- Verificar que los lectores se conectan correctamente
- Ver qué tags están detectando las antenas
- Diagnosticar problemas de conexión o configuración
- Probar antes de usar el gateway completo

## 📋 Requisitos

- Java 11 o superior
- Octane SDK en la carpeta `../Octane_SDK_Java_3_0_0/`
- Acceso de red a los lectores Impinj R220

## 🚀 Compilación y Ejecución

### Opción 1: Scripts Automáticos (Recomendado)

**Windows (PowerShell):**
```powershell
# Compilar todos los ejemplos
.\compilar.bat

# Ejecutar tests (se compila automáticamente si es necesario)
.\ejecutar.ps1 -IP 192.168.1.100 -Test TestConexion
.\ejecutar.ps1 -IP 192.168.1.100 -Test TestLecturaSimple
.\ejecutar.ps1 -IP 192.168.1.100 -Test TestTodasAntenas
.\ejecutar.ps1 -IP 192.168.1.100 -Test TestCompleto -Segundos 30
```

**Linux/Mac:**
```bash
# Dar permisos de ejecución
chmod +x compilar.sh

# Compilar
./compilar.sh

# Ejecutar
java -cp ".:../Octane_SDK_Java_3_0_0/lib/OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar" TestConexion 192.168.1.100
```

### Opción 2: Compilación Manual

**Windows (PowerShell):**
```powershell
# Compilar todos los ejemplos
javac -cp "../Octane_SDK_Java_3_0_0/lib/OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar" *.java

# Ejecutar
java -cp ".;../Octane_SDK_Java_3_0_0/lib/OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar" TestLecturaSimple 192.168.1.100
```

**Linux/Mac:**
```bash
# Compilar
javac -cp "../Octane_SDK_Java_3_0_0/lib/OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar" *.java

# Ejecutar
java -cp ".:../Octane_SDK_Java_3_0_0/lib/OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar" TestLecturaSimple 192.168.1.100
```

## 📁 Archivos

- **`TestConexion.java`** - Solo verifica conexión y muestra información del lector (sin leer tags)
  - Útil para verificar que el lector está accesible
  - Muestra modelo, firmware, número de antenas, configuración actual
  
- **`TestLecturaSimple.java`** - Ejemplo básico, lee tags de la antena 1
  - Ideal para empezar y verificar que detecta tags
  - Muestra EPC, antena y RSSI en tiempo real
  
- **`TestTodasAntenas.java`** - Prueba todas las antenas del lector
  - Habilita todas las antenas disponibles
  - Muestra estadísticas de tags por antena
  
- **`TestCompleto.java`** - Test completo con estadísticas detalladas
  - Información completa de cada tag detectado
  - Estadísticas de frecuencia de detección
  - Puede ejecutarse por tiempo determinado o hasta presionar ENTER

- **Scripts de ayuda:**
  - `compilar.bat` / `compilar.sh` - Compila todos los ejemplos
  - `ejecutar.ps1` - Script PowerShell para ejecutar fácilmente

## 🔧 Configuración

Edita los archivos `.java` y cambia:
- La IP del lector (o pásala como argumento)
- El número de puerto de antena
- La potencia de transmisión (TxPower)
- La sensibilidad de recepción (RxSensitivity)

## 📊 Interpretación de Resultados

- **Tags detectados**: El lector y las antenas funcionan correctamente
- **Sin tags**: Verificar que haya tags cerca de las antenas
- **Error de conexión**: Verificar IP, red y que el lector esté encendido
- **Error de configuración**: Verificar que los parámetros sean válidos

