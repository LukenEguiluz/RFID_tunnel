# 📖 Configuración de Lectura Intermitente

Esta guía explica cómo configurar un lector RFID para usar lectura intermitente en lugar de lectura continua.

## 🎯 ¿Qué es la Lectura Intermitente?

La lectura intermitente permite que el lector alterne entre períodos de lectura y pausa:
- **Lectura**: El lector lee tags durante X segundos
- **Pausa**: El lector se detiene durante Y segundos
- **Ciclo**: Se repite automáticamente

Esto es útil para:
- Reducir el consumo de energía
- Reducir la carga en la base de datos
- Lecturas programadas en intervalos específicos

## ⚙️ Configuración

### Opción 1: Por API REST (Recomendado)

#### Endpoint
```
POST /api/readers/{id}/intermittent
```

#### Ejemplo con cURL

**Activar lectura intermitente (5s lectura, 5s pausa):**
```bash
curl -X POST http://localhost:8080/api/readers/LECTOR_ID/intermittent \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "readDurationSeconds": 5,
    "pauseDurationSeconds": 5
  }'
```

**Activar lectura intermitente (10s lectura, 20s pausa):**
```bash
curl -X POST http://localhost:8080/api/readers/LECTOR_ID/intermittent \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "readDurationSeconds": 10,
    "pauseDurationSeconds": 20
  }'
```

**Desactivar lectura intermitente (volver a modo continuo):**
```bash
curl -X POST http://localhost:8080/api/readers/LECTOR_ID/intermittent \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": false
  }'
```

#### Ejemplo con PowerShell (Windows)

```powershell
$body = @{
    enabled = $true
    readDurationSeconds = 5
    pauseDurationSeconds = 5
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/readers/LECTOR_ID/intermittent" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body
```

#### Respuesta Exitosa
```json
{
  "message": "Configuración de lectura intermitente actualizada. Reinicia el lector para aplicar cambios.",
  "readerId": "LECTOR_ID",
  "intermittentEnabled": true,
  "readDurationSeconds": 5,
  "pauseDurationSeconds": 5
}
```

### Opción 2: Por SQL Directo

Si prefieres configurarlo directamente en la base de datos:

```sql
-- Activar lectura intermitente
UPDATE readers 
SET 
    intermittent_enabled = true,
    read_duration_seconds = 5,
    pause_duration_seconds = 5
WHERE id = 'LECTOR_ID';

-- Desactivar lectura intermitente (volver a modo continuo)
UPDATE readers 
SET intermittent_enabled = false
WHERE id = 'LECTOR_ID';
```

**Nota:** Después de actualizar por SQL, necesitas reiniciar el lector para que los cambios surtan efecto:
```bash
curl -X POST http://localhost:8080/api/readers/LECTOR_ID/reset
```

### Opción 3: Verificar Configuración Actual

Para ver la configuración actual de un lector:

```bash
curl http://localhost:8080/api/readers/LECTOR_ID
```

La respuesta incluirá:
- `intermittentEnabled`: true/false
- `readDurationSeconds`: duración de lectura
- `pauseDurationSeconds`: duración de pausa

## 🔄 Aplicar Cambios

Después de configurar la lectura intermitente, necesitas reiniciar el lector para aplicar los cambios:

```bash
# Opción 1: Reset (reconecta el lector)
curl -X POST http://localhost:8080/api/readers/LECTOR_ID/reset

# Opción 2: Stop y Start
curl -X POST http://localhost:8080/api/readers/LECTOR_ID/stop
curl -X POST http://localhost:8080/api/readers/LECTOR_ID/start
```

## 📊 Ejemplos de Uso

### Ejemplo 1: Lectura Rápida (2s lectura, 1s pausa)
Útil para lecturas frecuentes con pausas cortas:
```json
{
  "enabled": true,
  "readDurationSeconds": 2,
  "pauseDurationSeconds": 1
}
```

### Ejemplo 2: Lectura Moderada (5s lectura, 5s pausa)
Balance entre lectura y pausa:
```json
{
  "enabled": true,
  "readDurationSeconds": 5,
  "pauseDurationSeconds": 5
}
```

### Ejemplo 3: Lectura Espaciada (10s lectura, 30s pausa)
Útil para reducir carga y consumo:
```json
{
  "enabled": true,
  "readDurationSeconds": 10,
  "pauseDurationSeconds": 30
}
```

### Ejemplo 4: Lectura Continua (Desactivar intermitente)
Volver al comportamiento original:
```json
{
  "enabled": false
}
```

## ⚠️ Notas Importantes

1. **Valores Mínimos**: Los valores de duración deben ser mayores a 0 segundos
2. **Aplicación de Cambios**: Después de cambiar la configuración, reinicia el lector
3. **Estado del Lector**: El lector debe estar conectado para que la lectura intermitente funcione
4. **Modo Continuo**: Si `intermittentEnabled = false`, el lector usa lectura continua (comportamiento original)

## 🔍 Verificar Funcionamiento

Para verificar que la lectura intermitente está funcionando:

1. **Ver logs del gateway:**
   ```bash
   docker-compose logs -f gateway
   ```
   
   Deberías ver mensajes como:
   ```
   Iniciando fase de lectura para lector LECTOR_ID (5s)
   Iniciando fase de pausa para lector LECTOR_ID (5s)
   ```

2. **Verificar estado del lector:**
   ```bash
   curl http://localhost:8080/api/readers/LECTOR_ID/status
   ```
   
   El campo `reading` alternará entre `true` y `false` según el ciclo.

3. **Monitorear eventos:**
   Los eventos de tags solo se recibirán durante los períodos de lectura.

## 🐛 Solución de Problemas

### El lector no inicia lectura intermitente
- Verifica que `intermittentEnabled = true` en la base de datos
- Asegúrate de que el lector esté conectado (`isConnected = true`)
- Reinicia el lector después de cambiar la configuración

### La lectura no se detiene en las pausas
- Verifica los logs para ver si hay errores
- Asegúrate de que los valores de duración sean válidos (> 0)
- Intenta hacer un reset completo del lector

### Quiero volver a lectura continua
```bash
curl -X POST http://localhost:8080/api/readers/LECTOR_ID/intermittent \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'

curl -X POST http://localhost:8080/api/readers/LECTOR_ID/reset
```




