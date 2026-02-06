# Guía: VM como puente y webapp conectada al Gateway

Este documento es la referencia para **apuntar tu webapp al Gateway** usando una **VM** como conexión intermedia (con ZeroTier).

---

## Esquema

```
[Webapp (navegador)]  ──ZeroTier──►  [VM]  ◄──ZeroTier──  [Lectores RFID en sede]
                            │
                            ▼
                     Gateway en Docker (puerto 8080)
```

- **VM:** tiene ZeroTier y corre el Gateway en Docker.
- **Webapp:** en el navegador del usuario; se conecta a `http://<IP_VM>:8080` y `ws://<IP_VM>:8080/ws/events`.
- **Lectores:** en sede, también en ZeroTier; el Gateway (en la VM) se conecta a ellos por su hostname/IP.

---

## Parte 1: Qué necesitas en la VM

### Checklist

| Requisito | Cómo comprobarlo |
|-----------|-------------------|
| Docker | `docker --version` |
| Docker Compose | `docker compose version` o `docker-compose --version` |
| ZeroTier | `zerotier-cli listnetworks` |
| Proyecto RFID_tunnel | Carpeta con `docker-compose.yml`, `Dockerfile`, `src`, `pom.xml`, `Octane_SDK_Java_3_0_0` |

### Instalar Docker (si falta)

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
# Cerrar sesión y volver a entrar, o ejecutar con sudo
```

### Instalar ZeroTier (si falta)

```bash
curl -s https://install.zerotier.com | sudo bash
sudo zerotier-cli join <TU_NETWORK_ID>
# Autorizar el nodo en my.zerotier.com
```

### Firewall en la VM

El puerto **8080** debe estar permitido para que la webapp (y quien esté en ZeroTier) llegue al Gateway:

```bash
# Ubuntu (ufw)
sudo ufw allow 8080/tcp
sudo ufw reload

# O firewalld
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

---

## Parte 2: Subir el proyecto y arrancar el Gateway en la VM

### 1. Tener el proyecto en la VM

- Clonar el repo, o  
- Copiar la carpeta del proyecto (incluye `docker-compose.yml`, `Dockerfile`, `src`, `Octane_SDK_Java_3_0_0`, `pom.xml`).

```bash
cd /ruta/donde/quieras
git clone <url-repo> RFID_tunnel
cd RFID_tunnel
```

### 2. Iniciar el Gateway en Docker

```bash
docker compose up -d --build
```

(O si usas Compose v1: `docker-compose up -d --build`.)

El Gateway queda expuesto en el contenedor en el puerto **8080** y en la VM en **0.0.0.0:8080** (accesible por ZeroTier).

### 3. Anotar la IP ZeroTier de la VM

```bash
zerotier-cli listnetworks
```

Copia la IP asignada (ej. `10.147.20.10`). Es la que usarás en la webapp.

### 4. Comprobar que el Gateway responde

En la VM:

```bash
curl http://localhost:8080/api/health
```

Desde otro equipo en la misma red ZeroTier (o desde tu PC con ZeroTier):

```bash
curl http://10.147.20.10:8080/api/health
```

Debe devolver JSON con `"status":"UP"` (o similar).

---

## Parte 3: Apuntar la webapp al Gateway (vía la VM)

La webapp debe usar la **IP ZeroTier de la VM** (o su hostname si lo tienes configurado) como base del Gateway.

### URLs que debe usar la webapp

Sustituye `IP_VM` por la IP ZeroTier de tu VM (ej. `10.147.20.10`):

| Uso | URL |
|-----|-----|
| Base API REST | `http://IP_VM:8080` |
| Health | `http://IP_VM:8080/api/health` |
| Estado | `http://IP_VM:8080/api/status` |
| Eventos en tiempo real (SSE) | `http://IP_VM:8080/api/realtime/events` |
| **WebSocket** | `ws://IP_VM:8080/ws/events` |
| Sesiones (iniciar lectura) | `POST http://IP_VM:8080/api/sessions/start` |
| Sesiones (parar) | `POST http://IP_VM:8080/api/sessions/{id}/stop` |

### Variables de entorno en la webapp

Configura una sola variable con la base del Gateway; el resto se construye a partir de ella.

**Vite (React/Vue, etc.):**

```env
VITE_RFID_GATEWAY_URL=http://10.147.20.10:8080
```

**Create React App:**

```env
REACT_APP_RFID_GATEWAY_URL=http://10.147.20.10:8080
```

**Next.js (cliente):**

```env
NEXT_PUBLIC_RFID_GATEWAY_URL=http://10.147.20.10:8080
```

**Genérico (si usas otra cosa):**

```env
RFID_GATEWAY_URL=http://10.147.20.10:8080
```

### Uso en código (JavaScript/TypeScript)

```javascript
// Base URL del Gateway (vía la VM)
const GATEWAY_URL = import.meta.env.VITE_RFID_GATEWAY_URL   // Vite
  // || process.env.REACT_APP_RFID_GATEWAY_URL             // CRA
  // || process.env.NEXT_PUBLIC_RFID_GATEWAY_URL            // Next
  || 'http://10.147.20.10:8080';  // fallback

// WebSocket: sustituir http por ws
const GATEWAY_WS = GATEWAY_URL.replace(/^http/, 'ws') + '/ws/events';

// REST
const res = await fetch(`${GATEWAY_URL}/api/status`);
const data = await res.json();

// SSE
const eventSource = new EventSource(`${GATEWAY_URL}/api/realtime/events`);
eventSource.addEventListener('tag', (e) => {
  const tag = JSON.parse(e.data);
  console.log('Tag:', tag.epc, tag.rssi);
});

// WebSocket
const ws = new WebSocket(GATEWAY_WS);
ws.onmessage = (e) => {
  const msg = JSON.parse(e.data);
  if (msg.type === 'TAG_DETECTED') console.log('Tag:', msg.data.epc);
};
```

### Requisito para que funcione

- El **navegador** donde se abre la webapp debe poder alcanzar la IP de la VM (ej. `10.147.20.10`).
- Si la webapp es pública pero el Gateway está solo en ZeroTier: el usuario que quiera ver lecturas en tiempo real debe tener **ZeroTier instalado y unido a la misma red** en su PC; así su navegador podrá conectar a `http://IP_VM:8080` y `ws://IP_VM:8080/ws/events`.

---

## Resumen rápido

1. **En la VM:** Docker + ZeroTier + proyecto; `docker compose up -d --build`; firewall 8080 abierto.
2. **Anotar:** IP ZeroTier de la VM (`zerotier-cli listnetworks`).
3. **En la webapp:** `VITE_RFID_GATEWAY_URL=http://<IP_VM>:8080` (o la variable que uses) y construir las URLs de API y WebSocket a partir de esa base.
4. **Usuarios:** Misma red ZeroTier en el equipo donde abren la webapp para poder conectar al Gateway vía la VM.

Con esto tu webapp queda apuntando al Gateway a través de la VM.
