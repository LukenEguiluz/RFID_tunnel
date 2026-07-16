# Integration bundle

Despliegue del gateway y PostgreSQL en stacks separados, con volumen de datos independiente del contenedor de la app.

## Contenido

| Archivo | Uso |
|---------|-----|
| `docker-compose.db.yml` | Solo PostgreSQL + red + volumen |
| `docker-compose.app.yml` | Solo gateway (puerto host `38080`) |
| `deploy.sh` | Orquesta: `up`, `update-app`, `stop-app`, `stop-all` |
| `COPIAR_AL_REPO.sh` | Copia estos archivos a la raíz del repo |

## Uso

```bash
bash integration-bundle/COPIAR_AL_REPO.sh
./deploy.sh up
./deploy.sh update-app   # rebuild gateway; no toca Postgres
```

No uses `docker compose down -v` en el stack de DB si quieres conservar datos.
