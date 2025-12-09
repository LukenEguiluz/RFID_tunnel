# 📡 API en Tiempo Real - RFID Gateway

## 🎯 Descripción

Se ha agregado una API en tiempo real usando **Server-Sent Events (SSE)** para ver las lecturas de tags en tiempo real, además de mejoras en el logging y reset automático de lectores.

## ✨ Nuevas Funcionalidades

### 1. Reset Automático Antes de Conectar
- Los lectores ahora se detienen automáticamente antes de configurarse
- Esto asegura un estado limpio antes de iniciar el inventario
- Se puede hacer reset manual con el endpoint `/api/readers/{id}/reset`

### 2. Logging Mejorado
- Los tags detectados ahora se loguean con información completa:
  ```
  TAG DETECTADO - Lector: reader-1, EPC: E2001234567890, Antena: 1, RSSI: -45.5 dBm
  ```
- Logging detallado de configuración de antenas
- Información de conexión y desconexión

### 3. API en Tiempo Real (SSE)

#### Endpoint Principal: `/api/realtime/events`

**Stream de eventos en tiempo real usando Server-Sent Events**

```bash
# Conectar al stream
curl -N http://localhost:8080/api/realtime/events

# Filtrar por lector
curl -N "http://localhost:8080/api/realtime/events?readerId=reader-1"

# Filtrar por EPC
curl -N "http://localhost:8080/api/realtime/events?epc=E2001234567890"
```

**Formato de eventos:**
```json
event: tag
data: {"epc":"E2001234567890","readerId":"reader-1","antennaId":"reader-1-antenna-1","antennaPort":1,"rssi":-45.5,"detectedAt":"2025-12-03T02:30:15"}

event: connected
data: {"status":"connected","message":"Stream iniciado"}
```

#### Endpoint: `/api/realtime/events/latest`

**Obtener eventos recientes (no streaming)**

```bash
# Últimos 50 eventos
curl http://localhost:8080/api/realtime/events/latest

# Últimos 20 eventos de un lector específico
curl "http://localhost:8080/api/realtime/events/latest?readerId=reader-1&limit=20"

# Últimos eventos de un EPC específico
curl "http://localhost:8080/api/realtime/events/latest?epc=E2001234567890&limit=10"
```

#### Endpoint: `/api/realtime/stats`

**Estadísticas en tiempo real**

```bash
# Estadísticas generales
curl http://localhost:8080/api/realtime/stats

# Estadísticas de un lector específico
curl "http://localhost:8080/api/realtime/stats?readerId=reader-1"
```

**Respuesta:**
```json
{
  "lastMinute": 15,
  "lastHour": 450,
  "totalEvents": 12345,
  "timestamp": "2025-12-03T02:30:15"
}
```

### 4. Reset Manual de Lectores

#### Endpoint: `POST /api/readers/{id}/reset`

**Resetear y reconectar un lector**

```bash
curl -X POST http://localhost:8080/api/readers/reader-1/reset
```

**Respuesta:**
```json
{
  "message": "Reader reset and reconnecting",
  "readerId": "reader-1"
}
```

## 🔧 Uso en el Navegador

### JavaScript para SSE

```javascript
// Conectar al stream
const eventSource = new EventSource('http://localhost:8080/api/realtime/events');

// Escuchar eventos de tags
eventSource.addEventListener('tag', (event) => {
    const tagData = JSON.parse(event.data);
    console.log('Tag detectado:', tagData);
    // Actualizar UI, mostrar notificación, etc.
});

// Escuchar conexión
eventSource.addEventListener('connected', (event) => {
    console.log('Conectado al stream:', event.data);
});

// Manejar errores
eventSource.onerror = (error) => {
    console.error('Error en SSE:', error);
    // Reconectar después de un delay
    setTimeout(() => {
        eventSource.close();
        // Reconectar...
    }, 5000);
};
```

### Ejemplo Completo HTML

```html
<!DOCTYPE html>
<html>
<head>
    <title>RFID Tags en Tiempo Real</title>
</head>
<body>
    <h1>Tags Detectados en Tiempo Real</h1>
    <div id="tags"></div>
    
    <script>
        const tagsDiv = document.getElementById('tags');
        const eventSource = new EventSource('http://localhost:8080/api/realtime/events');
        
        eventSource.addEventListener('tag', (event) => {
            const tag = JSON.parse(event.data);
            const tagElement = document.createElement('div');
            tagElement.innerHTML = `
                <strong>EPC:</strong> ${tag.epc}<br>
                <strong>Lector:</strong> ${tag.readerId}<br>
                <strong>Antena:</strong> ${tag.antennaPort}<br>
                <strong>RSSI:</strong> ${tag.rssi} dBm<br>
                <strong>Hora:</strong> ${tag.detectedAt}<br>
                <hr>
            `;
            tagsDiv.insertBefore(tagElement, tagsDiv.firstChild);
        });
    </script>
</body>
</html>
```

## 📊 Ver Logs en Tiempo Real

Para ver los tags detectados en los logs del contenedor:

**En PowerShell (Windows):**
```powershell
# Ver logs del gateway
docker-compose logs -f gateway

# Filtrar solo tags detectados
docker-compose logs -f gateway | Select-String "TAG DETECTADO"

# Ver logs y filtrar en tiempo real
docker-compose logs -f gateway | Select-String -Pattern "TAG DETECTADO|ERROR|Conectando|Iniciando"
```

**En Linux/Mac:**
```bash
# Ver logs del gateway
docker-compose logs -f gateway

# Filtrar solo tags detectados
docker-compose logs -f gateway | grep "TAG DETECTADO"
```

## 🐛 Solución de Problemas

### No se ven tags en el stream

1. **Verificar que el lector esté conectado:**
   ```bash
   curl http://localhost:8080/api/readers/reader-1/status
   ```

2. **Verificar que esté leyendo:**
   - El campo `reading` debe ser `true`

3. **Verificar logs:**
   
   **PowerShell:**
   ```powershell
   docker-compose logs gateway | Select-String "TAG DETECTADO"
   ```
   
   **Linux/Mac:**
   ```bash
   docker-compose logs gateway | grep "TAG DETECTADO"
   ```

4. **Hacer reset del lector:**
   ```bash
   curl -X POST http://localhost:8080/api/readers/reader-1/reset
   ```

### El stream se desconecta

- SSE puede desconectarse por timeout
- El cliente debe manejar reconexión automática
- Ver ejemplo de JavaScript arriba

### No hay tags en la base de datos

- Verificar que las antenas estén configuradas
- Verificar que las antenas estén habilitadas
- Revisar logs para errores de configuración

## 🔄 Flujo Completo

1. **Conectar lector** → Se detiene cualquier operación previa
2. **Configurar settings** → Se aplican configuraciones de antenas
3. **Iniciar inventario** → El lector comienza a leer tags
4. **Tags detectados** → Se guardan en BD y se envían vía SSE/WebSocket
5. **Logging** → Cada tag se loguea con información completa

## 📝 Notas

- SSE es unidireccional (servidor → cliente)
- Para comunicación bidireccional, usar WebSocket (`ws://localhost:8080/ws/events`)
- Los eventos SSE también se envían vía WebSocket
- El stream SSE incluye los últimos 10 eventos al conectar

