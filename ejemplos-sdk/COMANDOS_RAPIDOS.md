# 🚀 Comandos Rápidos para 192.168.0.192

## 📋 Pasos Rápidos

### 1. Compilar (solo la primera vez)
```powershell
.\compilar.bat
```

### 2. Verificar Conexión
Verifica que el lector esté accesible y muestra su información:
```powershell
.\ejecutar.ps1 -IP 192.168.0.192 -Test TestConexion
```

### 3. Lectura Simple
Lee tags de la antena 1 en tiempo real (presiona ENTER para detener):
```powershell
.\ejecutar.ps1 -IP 192.168.0.192 -Test TestLecturaSimple
```

### 4. Probar Todas las Antenas
Habilita todas las antenas y muestra estadísticas:
```powershell
.\ejecutar.ps1 -IP 192.168.0.192 -Test TestTodasAntenas
```

### 5. Test Completo con Estadísticas
Lee durante 30 segundos y muestra estadísticas detalladas:
```powershell
.\ejecutar.ps1 -IP 192.168.0.192 -Test TestCompleto -Segundos 30
```

## 🔧 Comandos Manuales (sin script)

Si prefieres ejecutar directamente:

```powershell
# Compilar
javac -cp "../Octane_SDK_Java_3_0_0/lib/OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar" *.java

# Ejecutar TestConexion
java -cp ".;../Octane_SDK_Java_3_0_0/lib/OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar" TestConexion 192.168.0.192

# Ejecutar TestLecturaSimple
java -cp ".;../Octane_SDK_Java_3_0_0/lib/OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar" TestLecturaSimple 192.168.0.192

# Ejecutar TestTodasAntenas
java -cp ".;../Octane_SDK_Java_3_0_0/lib/OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar" TestTodasAntenas 192.168.0.192

# Ejecutar TestCompleto (30 segundos)
java -cp ".;../Octane_SDK_Java_3_0_0/lib/OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar" TestCompleto 192.168.0.192 30
```

## 📊 Qué Esperar

### TestConexion
- ✅ Muestra modelo, firmware, número de antenas
- ✅ Muestra configuración actual
- ✅ No lee tags, solo verifica conexión

### TestLecturaSimple
- 🏷️ Muestra cada tag detectado en tiempo real
- 📡 Solo lee de la antena 1
- ⏹️ Presiona ENTER para detener

### TestTodasAntenas
- 🏷️ Muestra tags de todas las antenas
- 📊 Estadísticas al final (tags por antena)
- ⏹️ Presiona ENTER para detener

### TestCompleto
- 🏷️ Muestra cada tag nuevo detectado
- 📊 Estadísticas completas al final
- ⏱️ Se detiene automáticamente después del tiempo especificado

## ❌ Solución de Problemas

### Error de conexión
- Verifica que la IP sea correcta: `192.168.0.192`
- Verifica que el lector esté encendido
- Verifica conectividad: `ping 192.168.0.192`

### No se detectan tags
- Verifica que haya tags cerca de las antenas
- Verifica que las antenas estén conectadas físicamente
- Prueba con `TestTodasAntenas` para verificar todas las antenas

### Error de compilación
- Verifica que Java esté instalado: `java -version`
- Verifica que el SDK esté en la ruta correcta
- Ejecuta `.\compilar.bat` de nuevo






