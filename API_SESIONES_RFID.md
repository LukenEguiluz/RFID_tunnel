# 📡 API de Sesiones RFID - Control por Comandos

Esta API permite que una webapp externa controle la lectura de tags RFID mediante comandos. La webapp puede iniciar una sesión de lectura, consultar los tags detectados, y detener la sesión para obtener la lista final.

## 🎯 Concepto

1. **Webapp envía comando** → Inicia lectura
2. **Gateway recopila tags** → Durante la sesión activa
3. **Webapp consulta tags** → Polling cada X segundos
4. **Webapp envía comando** → Detiene lectura
5. **Gateway responde** → Lista de tags únicos detectados

## 🔌 Endpoints

### 1. Iniciar Sesión de Lectura

Inicia una sesión de lectura para un lector específico.

```http
POST /api/sessions/start
Content-Type: application/json

{
  "readerId": "reader-1"
}
```

**Respuesta (201 Created)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "readerId": "reader-1",
  "status": "ACTIVE",
  "startTime": "2024-01-15T10:30:00.000",
  "message": "Sesión iniciada exitosamente"
}
```

**Errores**:
- `400 Bad Request`: `readerId` no proporcionado, lector no encontrado, o lector no conectado
- `409 Conflict`: Ya existe una sesión activa para ese lector

**Ejemplo con cURL**:
```bash
curl -X POST http://localhost:8080/api/sessions/start \
  -H "Content-Type: application/json" \
  -d '{"readerId": "reader-1"}'
```

**Ejemplo con PowerShell**:
```powershell
$body = @{
    readerId = "reader-1"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/sessions/start" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body

$sessionId = $response.sessionId
Write-Host "Sesión iniciada: $sessionId"
```

---

### 2. Consultar Estado de Sesión (Polling)

Consulta el estado actual de una sesión y los tags detectados hasta el momento.

```http
GET /api/sessions/{sessionId}
```

**Respuesta (200 OK)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "readerId": "reader-1",
  "status": "ACTIVE",
  "startTime": "2024-01-15T10:30:00.000",
  "endTime": null,
  "epcs": [
    "E200123456789012",
    "E200123456789013",
    "E200123456789014"
  ],
  "epcCount": 3,
  "totalReads": 15
}
```

**Campos**:
- `sessionId`: ID único de la sesión
- `readerId`: ID del lector
- `status`: Estado (`ACTIVE`, `STOPPED`, `COMPLETED`)
- `startTime`: Fecha/hora de inicio
- `endTime`: Fecha/hora de fin (null si está activa)
- `epcs`: Lista de EPCs únicos detectados (ordenados)
- `epcCount`: Número de EPCs únicos
- `totalReads`: Total de lecturas (puede haber duplicados)

**Errores**:
- `404 Not Found`: Sesión no encontrada

**Ejemplo con cURL**:
```bash
curl http://localhost:8080/api/sessions/550e8400-e29b-41d4-a716-446655440000
```

**Ejemplo con JavaScript (Polling cada 1 segundo)**:
```javascript
const sessionId = "550e8400-e29b-41d4-a716-446655440000";

const pollInterval = setInterval(async () => {
  try {
    const response = await fetch(`http://localhost:8080/api/sessions/${sessionId}`);
    const data = await response.json();
    
    console.log(`Tags detectados: ${data.epcCount}`);
    console.log(`EPCs: ${data.epcs.join(", ")}`);
    
    // Si la sesión está detenida, dejar de hacer polling
    if (data.status !== "ACTIVE") {
      clearInterval(pollInterval);
      console.log("Sesión finalizada. Tags finales:", data.epcs);
    }
  } catch (error) {
    console.error("Error al consultar sesión:", error);
  }
}, 1000); // Polling cada 1 segundo
```

---

### 3. Detener Sesión

Detiene una sesión activa y retorna la lista final de tags detectados.

```http
POST /api/sessions/{sessionId}/stop
```

**Respuesta (200 OK)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "STOPPED",
  "endTime": "2024-01-15T10:35:00.000",
  "epcs": [
    "E200123456789012",
    "E200123456789013",
    "E200123456789014"
  ],
  "epcCount": 3,
  "totalReads": 15,
  "message": "Sesión detenida exitosamente"
}
```

**Errores**:
- `404 Not Found`: Sesión no encontrada

**Ejemplo con cURL**:
```bash
curl -X POST http://localhost:8080/api/sessions/550e8400-e29b-41d4-a716-446655440000/stop
```

**Ejemplo con PowerShell**:
```powershell
$sessionId = "550e8400-e29b-41d4-a716-446655440000"
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/sessions/$sessionId/stop" `
  -Method POST

Write-Host "Sesión detenida. Tags detectados: $($response.epcCount)"
$response.epcs | ForEach-Object { Write-Host "  - $_" }
```

---

### 4. Listar Sesiones Activas

Lista todas las sesiones activas en el sistema.

```http
GET /api/sessions/active
```

**Respuesta (200 OK)**:
```json
{
  "sessions": [
    {
      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
      "readerId": "reader-1",
      "status": "ACTIVE",
      "startTime": "2024-01-15T10:30:00.000",
      "epcCount": 3,
      "totalReads": 15
    }
  ],
  "count": 1
}
```

**Ejemplo con cURL**:
```bash
curl http://localhost:8080/api/sessions/active
```

---

## 📋 Flujo Completo de Uso

### Ejemplo 1: Lectura Simple

```bash
# 1. Iniciar sesión
SESSION_RESPONSE=$(curl -s -X POST http://localhost:8080/api/sessions/start \
  -H "Content-Type: application/json" \
  -d '{"readerId": "reader-1"}')

SESSION_ID=$(echo $SESSION_RESPONSE | jq -r '.sessionId')
echo "Sesión iniciada: $SESSION_ID"

# 2. Esperar unos segundos mientras se leen tags
sleep 5

# 3. Consultar estado
curl http://localhost:8080/api/sessions/$SESSION_ID

# 4. Detener sesión y obtener lista final
curl -X POST http://localhost:8080/api/sessions/$SESSION_ID/stop
```

### Ejemplo 2: Lectura con Polling (JavaScript)

```javascript
async function readTags(readerId) {
  // 1. Iniciar sesión
  const startResponse = await fetch('http://localhost:8080/api/sessions/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ readerId })
  });
  
  const session = await startResponse.json();
  const sessionId = session.sessionId;
  console.log('Sesión iniciada:', sessionId);
  
  // 2. Polling cada 1 segundo
  const pollInterval = setInterval(async () => {
    const statusResponse = await fetch(`http://localhost:8080/api/sessions/${sessionId}`);
    const status = await statusResponse.json();
    
    console.log(`Tags detectados: ${status.epcCount}`);
    if (status.epcs.length > 0) {
      console.log('EPCs:', status.epcs);
    }
  }, 1000);
  
  // 3. Detener después de 10 segundos
  setTimeout(async () => {
    clearInterval(pollInterval);
    
    const stopResponse = await fetch(`http://localhost:8080/api/sessions/${sessionId}/stop`, {
      method: 'POST'
    });
    
    const final = await stopResponse.json();
    console.log('Sesión finalizada. Tags finales:', final.epcs);
    return final.epcs;
  }, 10000);
}

// Uso
readTags('reader-1');
```

### Ejemplo 3: Lectura con Botón (HTML + JavaScript)

```html
<!DOCTYPE html>
<html>
<head>
    <title>Control de Lectura RFID</title>
</head>
<body>
    <h1>Control de Lectura RFID</h1>
    
    <div>
        <label>Lector ID:</label>
        <input type="text" id="readerId" value="reader-1">
    </div>
    
    <div>
        <button id="startBtn" onclick="startReading()">Iniciar Lectura</button>
        <button id="stopBtn" onclick="stopReading()" disabled>Detener Lectura</button>
    </div>
    
    <div id="status"></div>
    <div id="tags"></div>
    
    <script>
        let sessionId = null;
        let pollInterval = null;
        
        async function startReading() {
            const readerId = document.getElementById('readerId').value;
            
            const response = await fetch('http://localhost:8080/api/sessions/start', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ readerId })
            });
            
            const session = await response.json();
            sessionId = session.sessionId;
            
            document.getElementById('startBtn').disabled = true;
            document.getElementById('stopBtn').disabled = false;
            document.getElementById('status').innerHTML = 'Leyendo...';
            
            // Polling cada 1 segundo
            pollInterval = setInterval(updateStatus, 1000);
        }
        
        async function updateStatus() {
            if (!sessionId) return;
            
            const response = await fetch(`http://localhost:8080/api/sessions/${sessionId}`);
            const status = await response.json();
            
            document.getElementById('status').innerHTML = 
                `Tags detectados: ${status.epcCount} | Total lecturas: ${status.totalReads}`;
            
            if (status.epcs.length > 0) {
                document.getElementById('tags').innerHTML = 
                    '<strong>EPCs:</strong><br>' + status.epcs.join('<br>');
            }
        }
        
        async function stopReading() {
            if (!sessionId) return;
            
            clearInterval(pollInterval);
            
            const response = await fetch(`http://localhost:8080/api/sessions/${sessionId}/stop`, {
                method: 'POST'
            });
            
            const final = await response.json();
            
            document.getElementById('startBtn').disabled = false;
            document.getElementById('stopBtn').disabled = true;
            document.getElementById('status').innerHTML = 
                `Sesión finalizada. Tags únicos: ${final.epcCount}`;
            document.getElementById('tags').innerHTML = 
                '<strong>EPCs finales:</strong><br>' + final.epcs.join('<br>');
            
            sessionId = null;
        }
    </script>
</body>
</html>
```

---

## ⚠️ Notas Importantes

1. **Una sesión por lector**: Solo puede haber una sesión activa por lector a la vez
2. **Tags únicos**: La lista de `epcs` contiene solo EPCs únicos (sin duplicados)
3. **Persistencia**: Los tags también se guardan en la base de datos (eventos normales)
4. **Sesiones en memoria**: Las sesiones se almacenan en memoria y se limpian después de 1 hora
5. **Lector debe estar conectado**: El lector debe estar conectado antes de iniciar una sesión

---

## 🔍 Solución de Problemas

### Error: "Ya existe una sesión activa"
- Detén la sesión existente antes de iniciar una nueva
- O consulta `/api/sessions/active` para ver sesiones activas

### Error: "Lector no está conectado"
- Verifica que el lector esté habilitado y conectado
- Consulta `/api/readers/{readerId}/status` para ver el estado

### No se detectan tags
- Verifica que haya tags RFID dentro del rango del lector
- Verifica que las antenas estén habilitadas
- Revisa los logs del gateway para ver si hay errores

---

## 📊 Comparación con Lectura Continua

| Característica | Lectura Continua | Sesiones Controladas |
|----------------|------------------|----------------------|
| Inicio | Automático al conectar | Por comando desde webapp |
| Control | Manual (start/stop) | Por comandos API |
| Tags | Se guardan en BD | Se guardan en BD + sesión |
| Consulta | Query a BD | Polling a API de sesión |
| Uso | Monitoreo continuo | Lecturas bajo demanda |




