# API y contratos

Base URL por defecto: `http://localhost:38080`  
WebSocket: `ws://localhost:38080/ws/events`

En la UI también hay una referencia en `/api-docs`.

---

## Salud

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/status` | Estado general |
| GET | `/api/ping` | Ping simple |

---

## Lectores

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/readers` | Listar |
| GET | `/api/readers/{id}` | Detalle |
| GET | `/api/readers/{id}/status` | Estado conexión/lectura |
| POST | `/api/readers` | Crear |
| PUT | `/api/readers/{id}` | Actualizar |
| DELETE | `/api/readers/{id}` | Eliminar |
| POST | `/api/readers/{id}/start` | Iniciar lectura |
| POST | `/api/readers/{id}/stop` | Detener lectura |
| POST | `/api/readers/{id}/reset` | Reset software |
| POST | `/api/readers/{id}/reboot` | Reboot |
| GET | `/api/readers/{id}/hardware` | Info hardware |
| POST | `/api/readers/{id}/antennas/discover` | Descubrir antenas |
| POST | `/api/readers/{id}/antennas/reset` | Reset antenas |

---

## Antenas

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/antennas` | Listar |
| GET | `/api/antennas/{id}` | Detalle |
| GET | `/api/antennas/reader/{readerId}` | Por lector |
| POST | `/api/antennas` | Crear |
| PUT | `/api/antennas/{id}` | Actualizar (potencia, etc.) |
| POST | `/api/antennas/{id}/reset` | Reset |

---

## Eventos y tiempo real

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/events` | Histórico (filtros: epc, reader, antenna, from, to, page, size) |
| GET | `/api/events/latest` | Últimos eventos |
| GET | `/api/realtime/events` | SSE stream |
| GET | `/api/realtime/events/latest` | Últimos (no stream) |
| GET | `/api/realtime/stats` | Estadísticas |

---

## Grupos

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/groups` | Listar |
| GET | `/api/groups/{id}` | Detalle |
| POST | `/api/groups` | Crear |
| PUT | `/api/groups/{id}` | Actualizar |
| DELETE | `/api/groups/{id}` | Eliminar |
| GET | `/api/groups/{id}/stats` | Estadísticas |

---

## Sesiones (modo túnel)

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/sessions/start` | Iniciar (`readerId` o `groupId`) |
| GET | `/api/sessions/{sessionId}` | Detalle |
| POST | `/api/sessions/{sessionId}/stop` | Detener |
| GET | `/api/sessions/active` | Sesiones activas |
| POST | `/api/sessions/force-reset` | Reset forzado |

---

## Inventario continuo

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/inventory-systems` | Listar sistemas |
| GET | `/api/inventory-systems/{id}` | Detalle |
| GET | `/api/inventory-systems/{id}/live` | Estado live + lectores |
| GET | `/api/inventory-systems/{id}/epcs/current` | EPCs presentes |
| GET | `/api/inventory-systems/{id}/epcs/all` | Todos (paginado) |
| GET | `/api/inventory-systems/{id}/events` | Eventos de presencia |
| GET | `/api/inventory-systems/{id}/epcs/{epc}/timeline` | Timeline de un EPC |

### Webhook inventario lista

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/inventory-systems/{id}/inventory-list-webhook` | Snapshot (mismo JSON que el POST) |
| POST | `.../inventory-list-webhook/sync` | Encola SYNC completo |
| POST | `.../inventory-list-webhook/test` | Test con tags TEST + POST |
| GET/POST | `.../inventory-list-webhook/capture` | Receptor de prueba local |

---

## Contrato webhook `INVENTORY_LIST_UPDATE`

POST JSON a la URL configurada del sistema.

```json
{
  "id": "evt_00000000800000000000000000000001",
  "type": "INVENTORY_LIST_UPDATE",
  "version": "1",
  "timestamp": "2026-06-10T12:00:00.000Z",
  "data": {
    "systemId": "uuid-del-sistema",
    "systemName": "IHT-almacen",
    "generatedAt": "2026-06-10T12:00:00.000Z",
    "count": 2,
    "epcs": ["424430303031313230303300", "E2801160600002033A2B2C3D5"],
    "tags": [
      {
        "epc": "424430303031313230303300",
        "rssi": -48.2,
        "proximity": "CERCA",
        "proximityLabel": "Cerca",
        "readerId": "IHT-1",
        "antennaPort": 1,
        "missedCycles": 0
      }
    ],
    "added": ["424430303031313230303300"],
    "removed": []
  }
}
```

### Campos importantes

| Campo | Notas |
|-------|--------|
| `id` | Fijo por sistema (editable en UI). Pensado para upsert en la webapp. |
| `epcs` | Lista completa de disponibles (`present=true`). |
| `tags` | Detalle por EPC: RSSI, proximidad, lector, `missedCycles`. |
| `added` / `removed` | Deltas del envío. En SYNC forzado van vacíos. |

### Proximidad por RSSI

| Zona | RSSI |
|------|------|
| CERCA | ≥ −55 dBm |
| MEDIA | −55 a −70 dBm |
| LEJOS | < −70 dBm |

### Headers opcionales (si hay secret HMAC)

- `Content-Type: application/json`
- `X-Timestamp`: epoch segundos
- `X-Signature`: `sha256=` + HMAC-SHA256 de `timestamp + "." + body`

---

## WebSocket `/ws/events`

Tipos relevantes:

| type | Uso |
|------|-----|
| `TAG_DETECTED` | Lectura (modo túnel) |
| `READER_DISCONNECTED` / `READER_RECONNECTED` | Estado lector |
| `INVENTORY_CYCLE_START` | Inicio de ciclo de inventario |
| `INVENTORY_EPC_ADD` | EPC entra a inventario |
| `INVENTORY_EPC_REMOVE` | EPC sale |
| `INVENTORY_LIST_UPDATE` | Mismo envelope que el webhook |

---

## UI principal

| Ruta | Pantalla |
|------|----------|
| `/` | Dashboard |
| `/readers` | Lectores |
| `/tags` | Tags / eventos |
| `/groups` | Grupos |
| `/inventory-systems` | Sistemas de inventario |
| `/inventory-systems/{id}/epcs` | Inventario live + demo |
| `/inventory-systems/{id}/webhook` | Config webhook + tests |
| `/inventory-systems/{id}/edit` | Editar sistema (ciclos, lectores) |
| `/api-docs` | Docs en UI |

### Demo (en inventario live)

Forms POST:

- `/inventory-systems/{id}/demo/pause-cycles`
- `/inventory-systems/{id}/demo/resume-cycles`
- `/inventory-systems/{id}/demo/mark-removed` (`epc`)
- `/inventory-systems/{id}/demo/mark-returned` (`epc`)
