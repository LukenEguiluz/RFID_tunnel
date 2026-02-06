#!/bin/bash

# Script para iniciar el gateway RFID con Docker (para la VM con ZeroTier)

echo "🚀 Iniciando RFID Gateway con Docker..."
echo ""

# Verificar que Docker esté instalado
if ! command -v docker &> /dev/null; then
    echo "❌ Docker no está instalado. Por favor instala Docker primero."
    exit 1
fi

# Usar docker compose (v2) o docker-compose (v1)
COMPOSE="docker compose"
if ! docker compose version &> /dev/null; then
    COMPOSE="docker-compose"
fi

echo "📦 Construyendo imagen del gateway..."
$COMPOSE build

echo ""
echo "🔧 Iniciando servicios (PostgreSQL y Gateway)..."
$COMPOSE up -d

echo ""
echo "⏳ Esperando a que los servicios estén listos..."
sleep 10

echo ""
echo "📊 Estado de los contenedores:"
$COMPOSE ps

# Intentar mostrar IP ZeroTier para que la webapp se conecte
ZT_IP=""
if command -v zerotier-cli &> /dev/null; then
    ZT_IP=$(zerotier-cli listnetworks 2>/dev/null | grep -oE '10\.[0-9.]+' | head -1)
fi
if [ -z "$ZT_IP" ]; then
    ZT_IP=$(hostname -I 2>/dev/null | awk '{print $1}')
fi
[ -z "$ZT_IP" ] && ZT_IP="<IP_DE_ESTA_VM>"

echo ""
echo "✅ Gateway iniciado y expuesto en el contenedor."
echo ""
echo "📍 URLs en esta máquina:"
echo "   - API REST:  http://localhost:8080/api"
echo "   - Estado:    http://localhost:8080/api/status"
echo "   - Health:    http://localhost:8080/api/health"
echo "   - WebSocket: ws://localhost:8080/ws/events"
echo ""
echo "📍 Para la webapp (misma red ZeroTier), usa la IP de esta VM:"
echo "   - API:       http://${ZT_IP}:8080"
echo "   - WebSocket: ws://${ZT_IP}:8080/ws/events"
echo ""
echo "📝 Ver logs:    $COMPOSE logs -f gateway"
echo "🛑 Detener:     $COMPOSE down"
echo ""







