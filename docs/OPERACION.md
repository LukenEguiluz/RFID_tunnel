# Operación del Gateway

## Arranque

```bash
mvn -DskipTests package
docker compose up -d
curl http://localhost:38080/api/health
```

Respuesta esperada:

```json
{"database":"UP","status":"UP"}
```

UI: `http://localhost:38080/`

## Servicios

| Servicio | Contenedor | Puerto host |
|----------|------------|-------------|
| PostgreSQL | `rfidgateway-postgres` | `5432` |
| Gateway | `rfidgateway` | `38080` → `8080` |

Volumen de datos: `rfid_tunnel_postgres_data` (persistente). No uses `docker compose down -v` salvo que quieras borrar la base.

## Comandos

```bash
docker compose ps
docker compose logs -f gateway
docker compose restart gateway
docker compose build gateway && docker compose up -d gateway
docker compose down
```

## Inventario continuo

- Ciclo = sistema completo (todos los lectores del cluster en orden).
- Tiempo del lector (`readerSlotSeconds`) dividido entre antenas habilitadas.
- `removed` tras N ciclos sin lectura (campo “Ciclos antes de removed” en editar sistema).
- Ciclo sin ninguna lectura: no se marcan salidas automáticas.

## Demo / mock

En `/inventory-systems/{id}/epcs`:

- Pausar / reanudar ciclos
- Marcar salida (`removed`) o regreso (`added`) manual

Si hay URL de webhook configurada, estos cambios disparan el POST.

## Acceso por red / ZeroTier

Si el gateway corre en una VM:

1. `docker compose up -d`
2. Obtener IP ZeroTier: `zerotier-cli listnetworks`
3. Usar `http://<IP_ZT>:38080` (o el puerto que hayas publicado)
4. WebSocket: `ws://<IP_ZT>:38080/ws/events`

Abrir el puerto en el firewall del host si aplica.

## Problemas frecuentes

| Síntoma | Qué revisar |
|---------|-------------|
| Gateway no arranca | `docker compose logs -f gateway` (Flyway, DB, puerto) |
| Lector no conecta | IP/hostname, red, logs |
| No hay tags | Antenas habilitadas, RSSI mínimo, sistema activo |
| Webhook falla | URL, respuesta HTTP en panel de webhook |

## Webhook (resumen)

Configuración en `/inventory-systems/{id}/webhook`:

- URL POST de la webapp
- ID de evento fijo (default `evt_00000000800000000000000000000001`)
- Secret HMAC opcional
- Botones: Forzar POST (SYNC), Test POST + tags TEST

Contrato JSON completo: [API.md](API.md).

## Checklist de entrega

1. `mvn -DskipTests package` OK
2. `docker compose up -d` OK
3. `GET /api/health` → UP
4. Contraseñas por defecto cambiadas si es entorno real
