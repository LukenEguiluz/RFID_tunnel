@echo off
REM Script para iniciar el gateway RFID con Docker (Windows)

echo 🚀 Iniciando RFID Gateway con Docker...
echo.

REM Verificar que Docker esté instalado
where docker >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Docker no está instalado. Por favor instala Docker Desktop primero.
    pause
    exit /b 1
)

REM Verificar que Docker Compose esté instalado
where docker-compose >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Docker Compose no está instalado. Por favor instala Docker Desktop primero.
    pause
    exit /b 1
)

echo 📦 Construyendo imagen del gateway...
docker-compose build

echo.
echo 🔧 Iniciando servicios (PostgreSQL y Gateway)...
docker-compose up -d

echo.
echo ⏳ Esperando a que los servicios estén listos...
timeout /t 10 /nobreak >nul

echo.
echo 📊 Estado de los contenedores:
docker-compose ps

echo.
echo ✅ Gateway iniciado!
echo.
echo 📍 URLs disponibles:
echo    - API REST: http://localhost:8080/api
echo    - Estado: http://localhost:8080/api/status
echo    - Health: http://localhost:8080/api/health
echo    - WebSocket: ws://localhost:8080/ws/events
echo.
echo 📝 Ver logs:
echo    docker-compose logs -f gateway
echo.
echo 🛑 Detener servicios:
echo    docker-compose down
echo.

pause





