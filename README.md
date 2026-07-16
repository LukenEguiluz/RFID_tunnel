# RFID Gateway

Gateway Spring Boot para operar lectores Impinj RFID, persistir eventos en PostgreSQL y exponer UI web, API REST y WebSocket.

## Requisitos

- Java 11+
- Maven 3.6+
- Docker y Docker Compose
- SDK Octane incluido en `Octane_SDK_Java_3_0_0/`

## Estructura

```text
src/                      Código de la aplicación
Octane_SDK_Java_3_0_0/    SDK Impinj (requerido para compilar)
docker-compose.yml        Postgres + gateway (arranque simple)
integration-bundle/       Despliegue app/DB por separado
docs/OPERACION.md         Operación y mantenimiento
docs/API.md               API REST, webhook y WebSocket
```

## Arranque rápido

```bash
mvn -DskipTests package
docker compose up -d
curl http://localhost:38080/api/health
```

- UI / API: `http://localhost:38080`
- Contenedor interno: puerto `8080`
- Puerto en host: `GATEWAY_HTTP_PORT` (default `38080`)

## Variables de entorno

| Variable | Default | Uso |
|----------|---------|-----|
| `DB_HOST` | `postgres` | Host PostgreSQL |
| `DB_PORT` | `5432` | Puerto PostgreSQL |
| `DB_NAME` | `rfidgateway` | Base de datos |
| `DB_USER` | `rfiduser` | Usuario |
| `DB_PASSWORD` | `changeme` | Contraseña |
| `GATEWAY_HTTP_PORT` | `38080` | Puerto publicado en el host |
| `SPRING_PROFILES_ACTIVE` | `prod` | Perfil Spring |

Cambia `DB_PASSWORD` antes de un entorno real.

## Funcionalidades

- Lectores y antenas Impinj
- Lectura RFID y persistencia de eventos
- Inventario continuo por sistema (cluster de lectores)
- Webhook de inventario lista (`INVENTORY_LIST_UPDATE`)
- UI web, API REST y WebSocket `/ws/events`
- Controles demo: pausar ciclos, salida/regreso manual

## Endpoints útiles

```bash
# Salud
curl http://localhost:38080/api/health

# Lectores
curl http://localhost:38080/api/readers

# Sistemas de inventario
curl http://localhost:38080/api/inventory-systems

# Snapshot webhook inventario lista
curl http://localhost:38080/api/inventory-systems/{uuid}/inventory-list-webhook
```

WebSocket: `ws://localhost:38080/ws/events`

Referencia completa: [docs/API.md](docs/API.md) (también `/api-docs` en la UI).

## Inventario continuo (resumen)

- El ciclo es del **sistema** (cluster), no de cada lector por separado.
- `readerSlotSeconds` se reparte entre las antenas habilitadas del lector.
- Una etiqueta pasa a `removed` tras N ciclos consecutivos sin lectura (configurable, default 3).
- Si un ciclo completo no tiene ninguna lectura, no se penaliza el inventario.

## Despliegue

### Simple (recomendado)

```bash
docker compose up -d
```

### App y DB por separado

```bash
bash integration-bundle/COPIAR_AL_REPO.sh
./deploy.sh up
./deploy.sh update-app   # rebuild solo gateway, sin tocar datos
```

Detalle: `integration-bundle/README.md` y `docs/OPERACION.md`.

## Comandos frecuentes

```bash
docker compose ps
docker compose logs -f gateway
docker compose build gateway && docker compose up -d gateway
docker compose down          # no borra datos
# docker compose down -v   # ¡borra el volumen de Postgres!
```

## Licencia

El SDK Impinj en `Octane_SDK_Java_3_0_0/` está sujeto a la licencia del fabricante.
