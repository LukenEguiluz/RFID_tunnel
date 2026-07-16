#!/usr/bin/env bash
# Fusiona este bundle en la raíz del repo (carpeta padre de integration-bundle/ por defecto).
# Otro destino: bash COPIAR_AL_REPO.sh /ruta/al/repo
# Si el repo es de root: sudo bash COPIAR_AL_REPO.sh
set -euo pipefail
REPO="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
install -m 0644 "$HERE/docker-compose.db.yml" "$REPO/docker-compose.db.yml"
install -m 0644 "$HERE/docker-compose.app.yml" "$REPO/docker-compose.app.yml"
install -m 0755 "$HERE/deploy.sh" "$REPO/deploy.sh"
install -d -m 0755 "$REPO/scripts"
install -m 0755 "$HERE/scripts-instalar-en-proyecto.sh" "$REPO/scripts/instalar-en-proyecto.sh"
install -d -m 0755 "$REPO/docs"
install -m 0644 "$HERE/docs-SOUL.md" "$REPO/docs/SOUL.md"
echo "Listo. Archivos instalados en: $REPO"
