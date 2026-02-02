# 🌐 Arquitectura WebApp + VPN ZeroTier (AWS)

Esta guía describe cómo la webapp debe recibir la información del RFID Gateway y la arquitectura de conexión usando una VM en AWS y ZeroTier para la VPN entre lectores/antenas y la webapp.

---

## 📐 Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLOUD (AWS)                                     │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │  VM AWS (EC2 o similar)                                                │  │
│  │  - ZeroTier instalado (nodo de la red virtual)                         │  │
│  │  - IP ZeroTier: 10.147.x.x (ejemplo)                                  │  │
│  │  - Docker: gateway + webapp (o solo webapp si el gateway está on-prem) │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                      │                                       │
│                                      │ ZeroTier VPN (malla P2P)              │
│                                      ▼                                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         │                             │                             │
         ▼                             ▼                             ▼
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│  Sede/Local 1   │         │  Sede/Local 2   │         │  Webapp (naveg.) │
│  - Gateway RFID │         │  - Gateway RFID │         │  - Browser       │
│  - ZeroTier     │         │  - ZeroTier     │         │  - Conecta a     │
│  - IP ZT: 10.x  │         │  - IP ZT: 10.x  │         │    IP pública    │
│  - Lectores     │         │  - Lectores     │         │    o IP ZeroTier │
└─────────────────┘         └─────────────────┘         └─────────────────┘
```

---

## 🔗 Cómo la WebApp Recibe la Información

La webapp puede consumir los datos del Gateway RFID de **tres formas**:

### 1. Server-Sent Events (SSE) – Recomendado para tiempo real

La webapp se suscribe a un stream de eventos en tiempo real.

**Endpoint:**
```
GET /api/realtime/events?readerId={id}&antenna={antennaId}
```

**Formato de conexión (JavaScript):**
```javascript
// Base URL: IP del gateway (ZeroTier o pública)
const GATEWAY_URL = 'http://10.147.20.1:8080';  // IP ZeroTier del gateway en sede

const eventSource = new EventSource(`${GATEWAY_URL}/api/realtime/events`);

eventSource.addEventListener('tag', (event) => {
  const data = JSON.parse(event.data);
  // data: { epc, readerId, antennaId, antennaPort, rssi, detectedAt }
  console.log('Tag detectado:', data);
  // Actualizar UI, guardar en estado, etc.
});

eventSource.addEventListener('connected', (event) => {
  console.log('Conectado al stream:', event.data);
});

eventSource.onerror = (err) => {
  console.error('Error SSE:', err);
  // Reintentar conexión
};
```

**Ventajas:** Unidireccional, bajo overhead, reconexión automática en muchos navegadores.

---

### 2. WebSocket – Bidireccional

Si la webapp necesita enviar comandos y recibir eventos en la misma conexión.

**Endpoint:**
```
ws://{GATEWAY_IP}:8080/ws/events
```

**Formato de conexión (JavaScript):**
```javascript
const GATEWAY_WS = 'ws://10.147.20.1:8080/ws/events';
const ws = new WebSocket(GATEWAY_WS);

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  // Estructura según EventWebSocketHandler
  if (data.type === 'TAG_DETECTED') {
    console.log('Tag:', data.epc, 'Antena:', data.antennaId);
  }
  if (data.type === 'READER_RECONNECTED') {
    console.log('Lector reconectado:', data.readerId);
  }
};

ws.onclose = () => {
  // Reintentar conexión
  setTimeout(() => connectWebSocket(), 5000);
};
```

---

### 3. REST + Polling – Para consultas bajo demanda

Para listados, estado y sesiones (no ideal para tiempo real continuo).

**Endpoints útiles:**
```
GET  /api/events?reader={id}&antenna={id}&page=0&size=50
GET  /api/events/latest?limit=20
GET  /api/readers/{id}/status
GET  /api/antennas
POST /api/sessions/start  (body: { readerId })
POST /api/sessions/{id}/stop
GET  /api/sessions/{id}
```

**Ejemplo de polling cada 2 segundos:**
```javascript
async function pollEvents() {
  const res = await fetch(`${GATEWAY_URL}/api/events/latest?limit=10`);
  const events = await res.json();
  // Procesar eventos
}

setInterval(pollEvents, 2000);
```

---

## 📡 Configuración de la URL del Gateway en la WebApp

La webapp debe poder configurar la URL base del Gateway según el entorno:

| Escenario                    | URL del Gateway                        | Ejemplo                         |
|-----------------------------|----------------------------------------|---------------------------------|
| Gateway en misma máquina    | `http://localhost:8080`                | Desarrollo local                |
| Gateway en sede vía ZeroTier| `http://10.147.20.x:8080`              | IP ZeroTier del gateway en sede |
| Gateway expuesto en AWS     | `https://api.rfid.tudominio.com`       | Reverse proxy en VM AWS         |
| CORS                        | Origen permitido en `CorsConfig`       | Dominio de la webapp            |

**Variable de entorno sugerida:**
```bash
VITE_RFID_GATEWAY_URL=http://10.147.20.1:8080   # Vite/React
# o
REACT_APP_RFID_GATEWAY_URL=http://10.147.20.1:8080
# o
NEXT_PUBLIC_RFID_GATEWAY_URL=http://10.147.20.1:8080
```

---

## 🔐 ZeroTier: Configuración para la VPN

### 1. Crear red en ZeroTier Central

1. Entra en [my.zerotier.com](https://my.zerotier.com)
2. Crea una red y anota el **Network ID** (16 caracteres)
3. En la red: **Settings** → permite **Access Control** (o auto-auth según política)

### 2. Instalar ZeroTier en cada nodo

**VM AWS:**
```bash
curl -s https://install.zerotier.com | sudo bash
sudo zerotier-cli join <NETWORK_ID>
```

**Gateway en sede (Linux):**
```bash
curl -s https://install.zerotier.com | sudo bash
sudo zerotier-cli join <NETWORK_ID>
```

**Windows (si el gateway corre en Windows):**
- Descargar e instalar desde [zerotier.com](https://www.zerotier.com/download/)

### 3. Autorizar nodos en ZeroTier Central

- En la red, en **Members**, marca **Auth** para cada nodo que se conecte
- Anota la IP asignada (ej. `10.147.20.1`, `10.147.20.2`)

### 4. Comunicación

- Los gateways en sedes tendrán IPs ZeroTier (ej. `10.147.20.1`, `10.147.20.2`)
- La VM en AWS tendrá otra IP ZeroTier (ej. `10.147.20.10`)
- La webapp, si corre en la VM AWS, usará `http://10.147.20.1:8080` para hablar con el gateway de la sede 1

---

## 🏗️ Topologías Posibles

### Opción A: Gateway en sede, WebApp en AWS

- **Sede:** Gateway RFID + ZeroTier (IP: 10.147.20.1)
- **AWS:** VM con webapp (Nginx/Node) + ZeroTier (IP: 10.147.20.10)
- **Flujo:** WebApp en AWS llama a `http://10.147.20.1:8080` vía ZeroTier
- **Usuario:** Accede a la webapp por dominio público (ej. `https://rfid.tudominio.com`)

### Opción B: Gateway y WebApp en la misma VM AWS

- **AWS:** VM con Docker: gateway + webapp
- **Sede:** Solo lectores RFID con IP accesible desde la VM (o VPN hacia la sede)
- **Usuario:** Accede por dominio público

### Opción C: Gateway en sede, WebApp en local (desarrollo)

- **Sede:** Gateway + ZeroTier
- **Dev:** ZeroTier en tu PC, webapp en localhost
- **Flujo:** Webapp en `localhost:3000` llama a `http://10.147.20.1:8080`

---

## ⚠️ Consideraciones de Seguridad y Red

1. **CORS:** El `CorsConfig` del gateway debe permitir el origen de la webapp (dominio o IP).
2. **Firewall:** Puertos 8080 (gateway) y 9993 (ZeroTier) abiertos donde corresponda.
3. **HTTPS:** En producción, usar reverse proxy (Nginx) con TLS para la webapp.
4. **ZeroTier:** La red es privada; el tráfico entre nodos va cifrado por ZeroTier.
5. **Timeout SSE/WebSocket:** Configurar heartbeats y reconexión en la webapp.

---

## 📋 Resumen de Endpoints para la WebApp

| Uso                  | Método | Endpoint                          |
|----------------------|--------|-----------------------------------|
| Eventos en tiempo real | GET  | `/api/realtime/events`            |
| WebSocket eventos    | WS     | `/ws/events`                      |
| Últimos eventos      | GET    | `/api/events/latest?limit=N`      |
| Estado del gateway   | GET    | `/api/status`                     |
| Reset antena por ID  | POST   | `/api/antennas/{antennaId}/reset` |
| Iniciar sesión       | POST   | `/api/sessions/start`             |
| Detener sesión       | POST   | `/api/sessions/{id}/stop`         |
| Estado de sesión     | GET    | `/api/sessions/{id}`              |

---

## 📄 Formato de Datos de Tag (para la WebApp)

Cada evento de tag tiene esta estructura:

```json
{
  "epc": "E200123456789012",
  "readerId": "reader-1",
  "antennaId": "reader-1-antenna-1",
  "antennaPort": 1,
  "rssi": -45.5,
  "detectedAt": "2024-01-15T10:30:00.123"
}
```

---

*Documento creado para el proyecto doHealth RFID Gateway – Arquitectura WebApp + ZeroTier.*
