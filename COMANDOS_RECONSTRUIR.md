# 🔧 Comandos para Reconstruir Docker Compose

## 📍 1. Obtener tu IP Local

```powershell
ipconfig | findstr /i "IPv4"
```

Busca la IP que empiece con `192.168.x.x` o `10.x.x.x` (esa es tu IP en la red local).

---

## 🐳 2. Reconstruir Gateway RFID (Principal)

```powershell
# Ir a la carpeta principal
cd C:\Users\Luken\Documents\doHealth\RFIDgateway

# Detener contenedores
docker-compose down

# Reconstruir e iniciar
docker-compose up -d --build

# Ver logs para verificar
docker-compose logs -f gateway
```

**O en un solo comando:**
```powershell
cd C:\Users\Luken\Documents\doHealth\RFIDgateway
docker-compose down
docker-compose up -d --build
```

---

## 🌐 3. Reconstruir Webapp de Prueba

```powershell
# Ir a la carpeta de webapp
cd C:\Users\Luken\Documents\doHealth\RFIDgateway\webapp-test

# Detener contenedores
docker-compose down

# Reconstruir e iniciar
docker-compose up -d --build

# Ver logs
docker-compose logs -f
```

**O en un solo comando:**
```powershell
cd C:\Users\Luken\Documents\doHealth\RFIDgateway\webapp-test
docker-compose down
docker-compose up -d --build
```

---

## 🚀 4. Reconstruir TODO (Gateway + Webapp)

```powershell
# Gateway
cd C:\Users\Luken\Documents\doHealth\RFIDgateway
docker-compose down
docker-compose up -d --build

# Webapp
cd webapp-test
docker-compose down
docker-compose up -d --build
```

---

## ✅ 5. Verificar que Todo Esté Corriendo

```powershell
# Ver contenedores del gateway
cd C:\Users\Luken\Documents\doHealth\RFIDgateway
docker-compose ps

# Ver contenedores de webapp
cd webapp-test
docker-compose ps

# O ver todos los contenedores
docker ps
```

---

## 🔍 6. Ver Logs

```powershell
# Logs del gateway
cd C:\Users\Luken\Documents\doHealth\RFIDgateway
docker-compose logs -f gateway

# Logs de la webapp
cd webapp-test
docker-compose logs -f
```

---

## 📝 Nota sobre la IP

Después de obtener tu IP, actualiza la webapp:
1. Abre `http://localhost:3000`
2. En el campo "URL del Gateway", ingresa: `http://TU_IP:8080`
3. Ejemplo: `http://192.168.0.189:8080`





