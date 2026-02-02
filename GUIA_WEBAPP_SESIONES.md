# 📱 Guía: Control de Lectura RFID desde Webapp

Esta guía explica cómo una webapp externa puede controlar la lectura de tags RFID usando la API de sesiones del Gateway.

## 🎯 Flujo de Operación

```
┌─────────────┐
│   Webapp    │
└──────┬──────┘
       │
       │ 1. POST /api/sessions/start
       │    { "readerId": "reader-1" }
       │
       ▼
┌─────────────────┐
│  Gateway RFID    │
│  - Inicia lectura│
│  - Recopila tags │
└──────┬──────────┘
       │
       │ 2. Polling cada X segundos
       │    GET /api/sessions/{sessionId}
       │
       ▼
┌─────────────┐
│   Webapp    │
│  - Muestra  │
│    tags     │
└──────┬──────┘
       │
       │ 3. POST /api/sessions/{sessionId}/stop
       │
       ▼
┌─────────────────┐
│  Gateway RFID    │
│  - Detiene      │
│  - Retorna lista│
└─────────────────┘
```

## 📋 Paso a Paso

### 1. Iniciar Sesión de Lectura

**Endpoint**: `POST /api/sessions/start`

**Request**:
```json
{
  "readerId": "reader-1"
}
```

**Response**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "readerId": "reader-1",
  "status": "ACTIVE",
  "startTime": "2024-01-15T10:30:00.000",
  "message": "Sesión iniciada exitosamente"
}

```

**Código JavaScript**:
```javascript
async function startReading(readerId) {
  const response = await fetch('http://localhost:8080/api/sessions/start', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ readerId })
  });
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || 'Error al iniciar sesión');
  }
  
  const session = await response.json();
  return session.sessionId; // Guardar para siguientes llamadas
}

// Uso
const sessionId = await startReading('reader-1');
console.log('Sesión iniciada:', sessionId);
```

---

### 2. Hacer Polling para Consultar Tags

**Endpoint**: `GET /api/sessions/{sessionId}`

**Response**:
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

**Código JavaScript con Polling**:
```javascript
let pollInterval = null;

function startPolling(sessionId, intervalMs = 1000) {
  pollInterval = setInterval(async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/sessions/${sessionId}`);
      
      if (!response.ok) {
        throw new Error(`Error ${response.status}`);
      }
      
      const status = await response.json();
      
      // Actualizar UI con tags detectados
      console.log(`Tags detectados: ${status.epcCount}`);
      console.log('EPCs:', status.epcs);
      
      // Si la sesión está detenida, dejar de hacer polling
      if (status.status !== 'ACTIVE') {
        stopPolling();
      }
      
    } catch (error) {
      console.error('Error en polling:', error);
      stopPolling();
    }
  }, intervalMs);
}

function stopPolling() {
  if (pollInterval) {
    clearInterval(pollInterval);
    pollInterval = null;
  }
}

// Uso
startPolling(sessionId, 1000); // Polling cada 1 segundo
```

---

### 3. Detener Sesión y Obtener Lista Final

**Endpoint**: `POST /api/sessions/{sessionId}/stop`

**Response**:
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

**Código JavaScript**:
```javascript
async function stopReading(sessionId) {
  // Detener polling primero
  stopPolling();
  
  const response = await fetch(`http://localhost:8080/api/sessions/${sessionId}/stop`, {
    method: 'POST'
  });
  
  if (!response.ok) {
    throw new Error(`Error ${response.status}`);
  }
  
  const final = await response.json();
  
  console.log('Sesión finalizada');
  console.log(`Tags únicos: ${final.epcCount}`);
  console.log('EPCs finales:', final.epcs);
  
  return final.epcs; // Lista de tags únicos
}

// Uso
const tags = await stopReading(sessionId);
```

---

## 🎨 Ejemplo Completo: Webapp HTML

He creado un archivo `ejemplo-webapp-sesiones.html` que incluye:

- ✅ Interfaz completa para iniciar/detener lectura
- ✅ Polling automático cada X segundos
- ✅ Visualización de tags en tiempo real
- ✅ Estadísticas (EPCs únicos, total lecturas, tiempo)
- ✅ Log de eventos
- ✅ Manejo de errores

**Para usar**:
1. Abre `ejemplo-webapp-sesiones.html` en un navegador
2. Configura la URL del gateway (por defecto: `http://localhost:8080`)
3. Ingresa el ID del lector
4. Haz clic en "Iniciar Lectura"
5. Observa los tags detectados en tiempo real
6. Haz clic en "Detener Lectura" para obtener la lista final

---

## 🔄 Flujo Completo con Ejemplo

```javascript
// 1. Iniciar sesión
const sessionId = await startReading('reader-1');

// 2. Iniciar polling (cada 1 segundo)
startPolling(sessionId, 1000);

// 3. Esperar X segundos (o hasta que el usuario detenga)
setTimeout(async () => {
  // 4. Detener sesión y obtener lista final
  const tags = await stopReading(sessionId);
  
  // 5. Procesar tags
  console.log('Tags finales:', tags);
  // Enviar a tu backend, mostrar en UI, etc.
}, 10000); // Esperar 10 segundos
```

---

## 📊 Ejemplo con React

```jsx
import React, { useState, useEffect } from 'react';

function RFIDReader({ readerId, gatewayUrl = 'http://localhost:8080' }) {
  const [sessionId, setSessionId] = useState(null);
  const [tags, setTags] = useState([]);
  const [isReading, setIsReading] = useState(false);
  const [stats, setStats] = useState({ epcCount: 0, totalReads: 0 });

  // Iniciar lectura
  const startReading = async () => {
    try {
      const response = await fetch(`${gatewayUrl}/api/sessions/start`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ readerId })
      });
      
      const session = await response.json();
      setSessionId(session.sessionId);
      setIsReading(true);
    } catch (error) {
      console.error('Error al iniciar:', error);
    }
  };

  // Detener lectura
  const stopReading = async () => {
    if (!sessionId) return;
    
    try {
      const response = await fetch(`${gatewayUrl}/api/sessions/${sessionId}/stop`, {
        method: 'POST'
      });
      
      const final = await response.json();
      setTags(final.epcs);
      setStats({ epcCount: final.epcCount, totalReads: final.totalReads });
      setIsReading(false);
      setSessionId(null);
    } catch (error) {
      console.error('Error al detener:', error);
    }
  };

  // Polling
  useEffect(() => {
    if (!sessionId || !isReading) return;

    const interval = setInterval(async () => {
      try {
        const response = await fetch(`${gatewayUrl}/api/sessions/${sessionId}`);
        const status = await response.json();
        
        setTags(status.epcs);
        setStats({ epcCount: status.epcCount, totalReads: status.totalReads });
        
        if (status.status !== 'ACTIVE') {
          setIsReading(false);
        }
      } catch (error) {
        console.error('Error en polling:', error);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [sessionId, isReading, gatewayUrl]);

  return (
    <div>
      <h2>Control de Lectura RFID</h2>
      
      <div>
        <button onClick={startReading} disabled={isReading}>
          Iniciar Lectura
        </button>
        <button onClick={stopReading} disabled={!isReading}>
          Detener Lectura
        </button>
      </div>
      
      <div>
        <p>Tags únicos: {stats.epcCount}</p>
        <p>Total lecturas: {stats.totalReads}</p>
      </div>
      
      <div>
        <h3>Tags Detectados:</h3>
        <ul>
          {tags.map((epc, index) => (
            <li key={index}>{epc}</li>
          ))}
        </ul>
      </div>
    </div>
  );
}

export default RFIDReader;
```

---

## ⚙️ Configuración Recomendada

### Intervalo de Polling

- **1 segundo (1000ms)**: Para actualizaciones en tiempo real
- **2-5 segundos**: Para reducir carga en el servidor
- **10 segundos**: Para aplicaciones que no requieren tiempo real

### Manejo de Errores

```javascript
async function safePolling(sessionId) {
  try {
    const response = await fetch(`http://localhost:8080/api/sessions/${sessionId}`);
    
    if (response.status === 404) {
      // Sesión no encontrada, detener polling
      stopPolling();
      return;
    }
    
    if (!response.ok) {
      throw new Error(`Error ${response.status}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Error en polling:', error);
    // Reintentar o notificar al usuario
  }
}
```

---

## 🔍 Verificación

Para verificar que todo funciona:

1. **Verifica que el lector esté conectado**:
   ```bash
   curl http://localhost:8080/api/readers/reader-1/status
   ```

2. **Inicia una sesión de prueba**:
   ```bash
   curl -X POST http://localhost:8080/api/sessions/start \
     -H "Content-Type: application/json" \
     -d '{"readerId": "reader-1"}'
   ```

3. **Consulta el estado**:
   ```bash
   curl http://localhost:8080/api/sessions/{sessionId}
   ```

4. **Detén la sesión**:
   ```bash
   curl -X POST http://localhost:8080/api/sessions/{sessionId}/stop
   ```

---

## 📝 Notas Importantes

1. **Una sesión por lector**: Solo puede haber una sesión activa por lector
2. **Tags únicos**: La lista `epcs` contiene solo EPCs únicos (sin duplicados)
3. **Polling continuo**: Mientras la sesión esté activa, haz polling para ver tags nuevos
4. **Detener siempre**: Asegúrate de detener la sesión cuando termines
5. **Manejo de errores**: Implementa manejo de errores para conexiones perdidas

---

## 🚀 Próximos Pasos

1. Abre `ejemplo-webapp-sesiones.html` en tu navegador
2. Prueba iniciar/detener sesiones
3. Observa cómo se actualizan los tags en tiempo real
4. Integra el código en tu webapp





