#!/usr/bin/env bash
# Despliegue Docker: DB y app separados. El volumen rfid_tunnel_postgres_data persiste
# al hacer down del gateway o rebuild (no se usa docker compose down -v).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

DB_COMPOSE="docker-compose.db.yml"
APP_COMPOSE="docker-compose.app.yml"

require_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "ERROR: no se encontró el comando 'docker'." >&2
    echo "" >&2
    echo "Instálalo en Ubuntu/Debian, por ejemplo:" >&2
    echo "  sudo apt-get update && sudo apt-get install -y docker.io docker-compose-v2" >&2
    echo "  sudo systemctl enable --now docker" >&2
    echo "  sudo usermod -aG docker \"\$USER\"   # luego: newgrp docker  (o cerrar sesión)" >&2
    exit 1
  fi
  if ! docker compose version >/dev/null 2>&1; then
    echo "ERROR: falta 'docker compose' (plugin Compose v2). Paquete: docker-compose-v2" >&2
    exit 1
  fi
}

usage() {
  echo "Uso: $0 {up|deploy|start|update-app|stop-app|stop-db|stop-all|logs|logs-db|status|pull|help}"
  echo ""
  echo "  up, deploy, start  Levanta Postgres, espera salud, construye y arranca el gateway."
  echo "  update-app         Solo reconstruye y reinicia el gateway (Postgres y datos intactos)."
  echo "  stop-app           Para solo el gateway (la base sigue)."
  echo "  stop-db            Para Postgres (el volumen de datos NO se borra)."
  echo "  stop-all           Para gateway y Postgres (sin -v: datos conservados)."
  echo "  logs [servicio]    Logs del gateway (por defecto: gateway)."
  echo "  logs-db            Logs de Postgres."
  echo "  status             Estado de ambos stacks."
  echo "  pull               Actualiza imagen de Postgres."
  echo ""
  echo "Variable opcional: GATEWAY_HTTP_PORT (por defecto 38080 en el host; el contenedor sigue en 8080)."
  echo "  Ej.: export GATEWAY_HTTP_PORT=8080  si quieres el clásico 8080 en el host y está libre."
  exit "${1:-0}"
}

wait_for_postgres() {
  echo "Esperando a PostgreSQL..."
  local i
  for i in $(seq 1 45); do
    if docker compose -f "$DB_COMPOSE" exec -T postgres pg_isready -U rfiduser -d rfidgateway >/dev/null 2>&1; then
      echo "PostgreSQL listo."
      sleep 3
      return 0
    fi
    sleep 2
  done
  echo "ERROR: PostgreSQL no respondió a tiempo." >&2
  return 1
}

cmd="${1:-up}"
if [[ "$cmd" != -h && "$cmd" != --help && "$cmd" != help ]]; then
  require_docker
fi

case "$cmd" in
  -h|--help|help)
    usage 0
    ;;
  up|deploy|start)
    echo "=== Base de datos ($DB_COMPOSE) ==="
    docker compose -f "$DB_COMPOSE" up -d
    wait_for_postgres
    echo "=== Gateway ($APP_COMPOSE) ==="
    docker compose -f "$APP_COMPOSE" up -d --build
    echo ""
    echo "Gateway:  http://localhost:${GATEWAY_HTTP_PORT:-38080}"
    echo "Postgres: localhost:5432 (rfiduser / rfidgateway)"
    echo "Volumen:  rfid_tunnel_postgres_data (persistente)"
    ;;
  update-app)
    echo "=== Asegurando Postgres ($DB_COMPOSE) ==="
    docker compose -f "$DB_COMPOSE" up -d
    wait_for_postgres
    echo "=== Reconstruyendo y reiniciando gateway ($APP_COMPOSE) ==="
    docker compose -f "$APP_COMPOSE" up -d --build
    echo ""
    echo "Gateway actualizado. Base de datos y volumen rfid_tunnel_postgres_data sin cambios."
    ;;
  stop-app)
    echo "Deteniendo solo el gateway..."
    docker compose -f "$APP_COMPOSE" down
    ;;
  stop-db)
    echo "Deteniendo Postgres (volumen conservado)..."
    docker compose -f "$DB_COMPOSE" stop
    ;;
  stop-all)
    echo "Deteniendo gateway y Postgres (sin -v)..."
    docker compose -f "$APP_COMPOSE" down
    docker compose -f "$DB_COMPOSE" down
    ;;
  logs)
    docker compose -f "$APP_COMPOSE" logs -f "${2:-gateway}"
    ;;
  logs-db)
    docker compose -f "$DB_COMPOSE" logs -f postgres
    ;;
  status)
    echo "--- Base de datos ---"
    docker compose -f "$DB_COMPOSE" ps -a
    echo ""
    echo "--- Gateway ---"
    docker compose -f "$APP_COMPOSE" ps -a
    ;;
  pull)
    docker compose -f "$DB_COMPOSE" pull
    ;;
  *)
    echo "Comando desconocido: $cmd" >&2
    usage 1
    ;;
esac
