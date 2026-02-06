# Iniciar el Gateway en la VM (conexión intermedia con ZeroTier)

La VM es la conexión intermedia: aquí corre el Gateway en Docker; la webapp (y los lectores en sede) se conectan a esta VM vía ZeroTier.

---

## Requisitos en la VM

- Docker (y Docker Compose v2: `docker compose` o `docker-compose`)
- ZeroTier instalado y unido a tu red (ya lo tienes)

---

## 1. Copiar el proyecto a la VM

Desde tu PC (donde está el repo), por ejemplo con SCP o clonando en la VM:

```bash
# En la VM, si clonas por git:
git clone <url-del-repo> RFID_tunnel
cd RFID_tunnel
```

O copia la carpeta del proyecto (incluyendo `docker-compose.yml`, `Dockerfile`, `src`, `Octane_SDK_Java_3_0_0`, `pom.xml`, etc.) a la VM.

---

## 2. Iniciar Docker y dejar el Gateway expuesto

En la VM, desde la raíz del proyecto:

```bash
chmod +x docker-start.sh
./docker-start.sh
```

O manualmente:

```bash
docker compose up -d --build
# o, si usas la versión antigua:
# docker-compose up -d --build
```

El puerto **8080** queda expuesto en **0.0.0.0** (todas las interfaces), así que el Gateway es accesible:

- En la misma VM: `http://localhost:8080`
- Desde la red ZeroTier: `http://<IP_ZEROTIER_DE_LA_VM>:8080`

---

## 3. Ver la IP ZeroTier de la VM

En la VM:

```bash
zerotier-cli listnetworks
```

Ahí verás la IP asignada (ej. `10.147.20.10`). Esa es la URL que debe usar la webapp:

- **REST/SSE:** `http://10.147.20.10:8080`
- **WebSocket:** `ws://10.147.20.10:8080/ws/events`

---

## 4. Comprobar que responde

En la VM o desde otro equipo en la misma red ZeroTier:

```bash
curl http://localhost:8080/api/health
# o con la IP ZeroTier:
curl http://10.147.20.10:8080/api/health
```

Debe devolver algo como `{"status":"UP",...}`.

---

## 5. Configurar la webapp

En la webapp (variables de entorno o config), pon la URL del Gateway con la **IP ZeroTier de la VM**:

- `VITE_RFID_GATEWAY_URL=http://10.147.20.10:8080` (o la IP que te dio ZeroTier)
- WebSocket en el código: `ws://10.147.20.10:8080/ws/events`

Así la webapp (en el navegador del usuario) se conecta al Gateway que corre en el contenedor de tu VM.

---

## 6. Comandos útiles

| Acción              | Comando |
|---------------------|--------|
| Ver contenedores    | `docker compose ps` |
| Ver logs del gateway| `docker compose logs -f gateway` |
| Detener todo       | `docker compose down` |
| Reiniciar gateway  | `docker compose restart gateway` |

---

## Firewall (si aplica)

Si en la VM tienes firewall (ufw, firewalld, etc.), permite el puerto 8080:

```bash
# ufw
sudo ufw allow 8080/tcp
sudo ufw reload

# firewalld
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

Con esto, el Docker ya está iniciado y el Gateway queda expuesto en tu contenedor para que la webapp y los lectores se conecten vía ZeroTier.
