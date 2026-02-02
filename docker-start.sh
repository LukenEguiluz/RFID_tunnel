#!/bin/bash

# Script para iniciar el gateway RFID con Docker

echo "🚀 Iniciando RFID Gateway con Docker..."
echo ""

# Verificar que Docker esté instalado
if ! command -v docker &> /dev/null; then
    echo "❌ Docker no está instalado. Por favor instala Docker primero."
    exit 1
fi

# Verificar que Docker Compose esté instalado
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose no está instalado. Por favor instala Docker Compose primero."
    exit 1
fi

echo "📦 Construyendo imagen del gateway..."
docker-compose build

echo ""
echo "🔧 Iniciando servicios (PostgreSQL y Gateway)..."
docker-compose up -d

echo ""
echo "⏳ Esperando a que los servicios estén listos..."
sleep 10

echo ""
echo "📊 Estado de los contenedores:"
docker-compose ps

echo ""
echo "✅ Gateway iniciado!"
echo ""
echo "📍 URLs disponibles:"
echo "   - API REST: http://localhost:8080/api"
echo "   - Estado: http://localhost:8080/api/status"
echo "   - Health: http://localhost:8080/api/health"
echo "   - WebSocket: ws://localhost:8080/ws/events"
echo ""
echo "📝 Ver logs:"
echo "   docker-compose logs -f gateway"
echo ""
echo "🛑 Detener servicios:"
echo "   docker-compose down"
echo ""







