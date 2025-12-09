# Script para reconstruir Gateway y Webapp

Write-Host "🔧 Reconstruyendo Gateway RFID..." -ForegroundColor Cyan
cd $PSScriptRoot
docker-compose down
docker-compose up -d --build

Write-Host "`n🌐 Reconstruyendo Webapp de Prueba..." -ForegroundColor Cyan
cd webapp-test
docker-compose down
docker-compose up -d --build

Write-Host "`n✅ Reconstrucción completada!" -ForegroundColor Green
Write-Host "`n📍 Tu IP local es: 192.168.0.189" -ForegroundColor Yellow
Write-Host "🌐 Gateway: http://192.168.0.189:8080" -ForegroundColor Yellow
Write-Host "🧪 Webapp: http://localhost:3000" -ForegroundColor Yellow
Write-Host "`n📝 Asegúrate de usar la IP en la webapp, NO localhost!" -ForegroundColor Magenta

cd ..

