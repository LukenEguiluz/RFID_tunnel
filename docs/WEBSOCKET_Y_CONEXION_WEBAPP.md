# WebSocket, envío de información y conexión VM ↔ WebApp serverless

Este documento explica cómo funciona el envío de lecturas RFID (WebSocket/SSE), las validaciones recomendadas (umbral RSSI) y qué hacer en la VM para que la webapp serverless se conecte al Gateway.

---

## 1. Cómo funciona el envío de información

### Flujo desde el lector hasta la webapp

```
Lector RFID (Octane SDK)
    → GatewayTagReportListener.onTagReported()
    → [opcional: filtro por RSSI]
    → TagEventService.processTagEvent()
        → Guarda en BD (TagEventRepository)
        → WebSocketEventService.notifyTagDetected()  → EventWebSocketHandler → todos los clientes WS
        → RealtimeEventController.broadcastEvent()   → todos los clientes SSE
```

- **GatewayTagReportListener**: recibe los reportes del SDK por lector, extrae EPC, antena, RSSI y fase. Si está configurado el umbral RSSI, ignora las lecturas con señal por debajo del umbral. El resto se envían a `TagEventService.processTagEvent()`.
- **TagEventService**: persiste el evento en base de datos y notifica en tiempo real por dos canales:
  - **WebSocket** (`/ws/events`): envía un mensaje JSON a todos los clientes conectados.
  - **SSE** (`/api/realtime/events`): envía el mismo evento a todos los emisores SSE.

Ambos canales reciben la misma información (EPC, readerId, antennaId, antennaPort, rssi, phase, timestamp). No hay cola intermedia: cada lectura válida se guarda y se emite de inmediato.

### ¿Es funcional?

Sí. El flujo está implementado y operativo:

- Las lecturas se guardan en BD.
- Los clientes WebSocket reciben cada tag con `type: "TAG_DETECTED"` y `data` con epc, readerId, antennaId, antennaPort, rssi, phase.
- Los clientes SSE reciben eventos con nombre `tag` y el mismo payload en formato JSON.

Si no ves eventos en la webapp, revisa: que el lector esté conectado y leyendo, que la webapp apunte a la URL correcta del Gateway (ver sección 3) y que CORS/firewall permitan la conexión.

---

## 2. Validación por umbral de RSSI (potencia)

### Problema

El lector puede reportar tags lejanos o con señal muy débil (RSSI muy negativo, ej. -80 dBm). Eso puede generar ruido, falsos positivos o lecturas que no quieres usar en túnel/pasillo.

### Solución implementada: umbral mínimo de RSSI

Se añadió un **filtro configurable**: si la lectura tiene RSSI **por debajo** del umbral (más negativo = más débil), se **ignora**: no se guarda en BD ni se envía por WebSocket/SSE.

- **RSSI** está en dBm (negativo). Ejemplos orientativos:
  - -40 a -55 dBm: muy cerca.
  - -55 a -65 dBm: distancia media/correcta para túnel.
  - -70 a -80 dBm: lejos o señal débil.
  - Por debajo de -80 dBm: muy lejos o interferencias.

### Cómo configurarlo

En `application.yml` (o variables de entorno en producción):

```yaml
rfid:
  rssi-min-dbm: -70
```

- **-70**: ignorar lecturas con RSSI &lt; -70 dBm (recomendado para filtrar tags lejanos).
- **-65**: más estricto (solo señales más fuertes).
- **Vacío o sin configurar**: no se aplica filtro; se aceptan todas las lecturas.

En Docker/producción puedes usar variable de entorno:

```bash
RFID_RSSI_MIN_DBM=-70
```

y en `application-prod.yml`:

```yaml
rfid:
  rssi-min-dbm: ${RFID_RSSI_MIN_DBM:}
```

### Recomendación

- Empezar **sin umbral** (dejar `rssi-min-dbm` vacío) y revisar en logs o en la webapp qué RSSI tienen las lecturas “buenas” (túnel) y las “malas” (lejos).
- Definir un valor en el que se separen bien (ej. -70) y configurarlo. Ajustar según ambiente y antenas.

---

## 3. WebSocket en detalle

### Endpoint

- **URL:** `ws://<IP_O_DOMINIO_DEL_GATEWAY>:8080/ws/events`
- **Ejemplo con IP ZeroTier:** `ws://10.147.20.1:8080/ws/events`
- **Ejemplo con dominio:** `wss://gateway.tudominio.com/ws/events` (si hay reverse proxy con TLS).

### Comportamiento

- **Conexión:** el cliente abre una conexión WebSocket a `/ws/events`. El servidor la acepta y la guarda en un mapa de sesiones.
- **Dirección:** solo **servidor → cliente**. El servidor envía eventos; el cliente no envía comandos por este canal (iniciar/parar lectura se hace por REST: sesiones, lectores, etc.).
- **Eventos que se envían:**
  - `TAG_DETECTED`: cada lectura válida (después del filtro RSSI si está activo).
  - `READER_DISCONNECTED`: cuando se pierde la conexión con un lector.
  - `READER_RECONNECTED`: cuando un lector se reconecta.

### Formato del mensaje (TAG_DETECTED)

```json
{
  "type": "TAG_DETECTED",
  "timestamp": "2025-02-05T12:00:00.123Z",
  "data": {
    "epc": "E2001234567890",
    "readerId": "reader-1",
    "antennaId": "reader-1-antenna-1",
    "antennaPort": 1,
    "rssi": -52.5,
    "phase": 0.78
  }
}
```

### Ejemplo de uso en la webapp (JavaScript)

```javascript
const GATEWAY_WS = 'ws://10.147.20.1:8080/ws/events';  // Sustituir por la URL de tu Gateway
const ws = new WebSocket(GATEWAY_WS);

ws.onopen = () => console.log('WebSocket conectado');
ws.onmessage = (event) => {
  const msg = JSON.parse(event.data);
  if (msg.type === 'TAG_DETECTED') {
    console.log('Tag:', msg.data.epc, 'RSSI:', msg.data.rssi, 'Antena:', msg.data.antennaPort);
    // Actualizar UI, listas, etc.
  }
  if (msg.type === 'READER_RECONNECTED') console.log('Lector reconectado:', msg.data.readerId);
  if (msg.type === 'READER_DISCONNECTED') console.log('Lector desconectado:', msg.data.readerId);
};
ws.onerror = (e) => console.error('WebSocket error', e);
ws.onclose = () => {
  console.log('WebSocket cerrado, reconectar en 5s...');
  setTimeout(() => location.reload(), 5000);  // o reabrir ws
};
```

### SSE como alternativa

- **URL:** `GET /api/realtime/events` (mismo contenido que WebSocket para tags).
- Es unidireccional (solo servidor → cliente), sin necesidad de mantener un socket abierto desde el servidor; útil en entornos donde WebSocket esté bloqueado.

---

## 4. Qué hacer en la VM para que la webapp serverless se conecte

Tu webapp es **serverless** (p. ej. Vercel, Netlify, S3+CloudFront): solo sirve HTML/JS/CSS. La **conexión real** (WebSocket/SSE y REST) la hace el **navegador del usuario** contra el **Gateway**. El Gateway debe estar accesible desde ese navegador.

### Dónde corre qué

- **VM (con VPN, p. ej. ZeroTier):** aquí corre el **Gateway** (Java/Spring con WebSocket y SSE). La VM tiene una IP en la VPN (ej. `10.147.20.10`).
- **Webapp serverless:** solo entrega la aplicación frontend. No “se conecta” al Gateway; quien se conecta es el **navegador** del usuario que ha abierto esa webapp.

Por tanto, “conectar la VM a la webapp” en realidad significa: **hacer que el Gateway en la VM sea alcanzable por el navegador** (por VPN o por internet).

### Caso A: Usuarios en la misma VPN (ZeroTier)

- Instala ZeroTier en la VM y únela a la misma red que los lectores.
- En la VM: ejecuta el Gateway (Docker o Java) escuchando en `0.0.0.0:8080`.
- En la webapp (variable de entorno o config build-time), configura la **URL base del Gateway** con la **IP ZeroTier de la VM**, por ejemplo:
  - REST/SSE: `http://10.147.20.10:8080`
  - WebSocket: `ws://10.147.20.10:8080/ws/events`
- Cada usuario que abre la webapp debe tener **ZeroTier instalado y unido a la misma red** en su PC. Así su navegador puede abrir `ws://10.147.20.10:8080/ws/events` y hablar con el Gateway en la VM.

**Resumen en la VM:**

1. ZeroTier instalado y unido a la red.
2. Gateway corriendo (puerto 8080).
3. Firewall de la VM que permita tráfico entrante en 8080 desde la red ZeroTier (o todo si es red interna).
4. En la webapp, URL del Gateway = `http://<IP_ZEROTIER_VM>:8080` y WebSocket = `ws://<IP_ZEROTIER_VM>:8080/ws/events`.

### Caso B: Usuarios por internet (sin VPN)

- La VM debe ser accesible por internet (IP pública o dominio).
- En la VM:
  1. Gateway escuchando en 8080 (o detrás de un reverse proxy).
  2. Nginx (o similar) como reverse proxy: HTTPS (443) y WSS (WebSocket seguro) apuntando al Gateway en 8080.
  3. Firewall: abrir 443 (y opcionalmente 80 para redirigir a HTTPS).
- En la webapp, URL del Gateway = `https://gateway.tudominio.com` y WebSocket = `wss://gateway.tudominio.com/ws/events`.

En ambos casos, **iniciar/parar lectura y comandos** se hacen por **REST** (desde la misma webapp), por ejemplo:

- `POST /api/sessions/start` (body: `{ "readerId": "..." }`)
- `POST /api/sessions/{id}/stop`
- `GET /api/readers`, `GET /api/readers/{id}/status`, etc.

El WebSocket solo sirve para **recibir** eventos en tiempo real (tags, desconexión/reconexión de lectores).

### Resumen rápido

| Dónde está el usuario | Qué hacer en la VM | URL Gateway en la webapp |
|------------------------|---------------------|---------------------------|
| Misma VPN (ZeroTier)   | Gateway en 8080, ZeroTier en la VM, firewall 8080 | `http://<IP_ZEROTIER_VM>:8080` y `ws://.../ws/events` |
| Internet               | Gateway + Nginx (HTTPS/WSS), dominio, firewall 443 | `https://gateway.tudominio.com` y `wss://.../ws/events` |

La webapp serverless no necesita “conectarse” a nada desde el servidor; solo debe tener configurada la URL correcta del Gateway para que el navegador del usuario abra WebSocket/SSE y llame a la API REST.
