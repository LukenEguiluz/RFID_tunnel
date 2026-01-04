# 🔍 Diagnóstico: No se detectan tags

## Problema
Los lectores están conectados y muestran "leyendo exitosamente", pero no se detectan tags RFID.

## Cambios Realizados

### 1. Logging Mejorado
- Agregado logging detallado en `GatewayTagReportListener` para ver si se reciben eventos
- Verificación de estado antes y después de iniciar lectura
- Logging de configuración de reporte

### 2. Orden de Configuración Corregido
- El listener ahora se configura en el orden correcto (igual que el ejemplo que funciona)
- Aumentado tiempo de espera antes de iniciar (1 segundo)

## Pasos para Diagnosticar

### 1. Verificar que hay tags físicos
**IMPORTANTE:** Asegúrate de que hay tags RFID físicos cerca del lector (dentro del rango de lectura).

### 2. Reconstruir y revisar logs
```powershell
# Reconstruir el gateway
docker-compose build gateway
docker-compose up -d

# Ver logs en tiempo real
docker-compose logs -f gateway

# Buscar mensajes específicos
docker-compose logs gateway | Select-String -Pattern "onTagReported|TAG DETECTADO|Singulating|Estado del lector"
```

### 3. Verificar estado del lector
```powershell
# Ver estado del lector
Invoke-RestMethod -Uri "http://localhost:8080/api/readers/{ID_DEL_LECTOR}/status"
```

### 4. Probar con el ejemplo que funciona
Para comparar, ejecuta el ejemplo que SÍ funciona:
```powershell
cd ejemplos-sdk
java -cp ".;..\Octane_SDK_Java_3_0_0\lib\OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar" TestLecturaSimple 192.168.0.192
```

Si el ejemplo funciona pero el gateway no, entonces hay una diferencia en la configuración.

### 5. Hacer reset del lector
Si el lector está conectado pero no lee, intenta hacer reset:
```powershell
# Reset rápido
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/readers/{ID_DEL_LECTOR}/reset"

# O reboot completo
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/readers/{ID_DEL_LECTOR}/reboot"
```

## Qué Buscar en los Logs

### Logs Esperados (si todo funciona):
```
INFO - Configurando listeners para lector...
INFO - Listeners configurados correctamente
INFO - Aplicando configuración al lector...
INFO - Configuración de reporte: Mode=Individual, IncludeAntenna=true, IncludeRSSI=true
INFO - Estado del lector antes de iniciar: Singulating=false, Connected=true
INFO - Iniciando inventario en lector...
INFO - Estado del lector después de iniciar: Singulating=true, Connected=true
INFO - onTagReported llamado - Lector: ..., Tags en reporte: X
INFO - TAG DETECTADO - Lector: ..., EPC: ..., Antena: ..., RSSI: ... dBm
```

### Si NO ves "onTagReported llamado":
- El listener no se está registrando correctamente
- El lector no está enviando reportes
- Hay un problema con la configuración del reporte

### Si ves "onTagReported llamado" pero "Tags en reporte: 0":
- El lector está funcionando pero no hay tags en el rango
- Verifica que hay tags físicos cerca

### Si ves "Singulating=false" después de iniciar:
- El lector no está iniciando correctamente
- Puede haber un error en la configuración

## Comparación con el Ejemplo que Funciona

El ejemplo `TestLecturaSimple.java` que SÍ funciona tiene este orden:
1. Conectar
2. Obtener configuración por defecto
3. Configurar settings (modo, reporte, antenas)
4. **Configurar listener**
5. Aplicar settings
6. Iniciar lectura

El gateway ahora sigue el mismo orden.

## Próximos Pasos

1. **Reconstruir el gateway** con los cambios
2. **Revisar los logs** para ver los nuevos mensajes de diagnóstico
3. **Verificar que hay tags físicos** cerca del lector
4. **Comparar con el ejemplo** que funciona
5. **Reportar los logs** si el problema persiste



