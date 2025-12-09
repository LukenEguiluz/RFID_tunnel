# 🐳 Guía de Uso con Docker

## Requisitos Previos

- Docker Desktop instalado (Windows/Mac) o Docker + Docker Compose (Linux)
- Al menos 2GB de RAM disponible
- Puerto 8080 y 5432 libres

## 🚀 Inicio Rápido

### Opción 1: Script Automático (Recomendado)

**Windows:**
```bash
docker-start.bat
```

**Linux/Mac:**
```bash
chmod +x docker-start.sh
./docker-start.sh
```

### Opción 2: Comandos Manuales

```bash
# 1. Construir la imagen
docker-compose build

# 2. Iniciar servicios
docker-compose up -d

# 3. Ver logs
docker-compose logs -f gateway
```

## 📋 Comandos Útiles

### Ver Estado de Contenedores
```bash
docker-compose ps
```

### Ver Logs

**PowerShell (Windows):**
```powershell
# Todos los servicios
docker-compose logs -f

# Solo el gateway
docker-compose logs -f gateway

# Solo PostgreSQL
docker-compose logs -f postgres

# Filtrar logs (equivalente a grep)
docker-compose logs gateway | Select-String "TAG DETECTADO"
docker-compose logs gateway | Select-String -Pattern "ERROR|WARN"
```

**Linux/Mac:**
```bash
# Todos los servicios
docker-compose logs -f

# Solo el gateway
docker-compose logs -f gateway

# Solo PostgreSQL
docker-compose logs -f postgres

# Filtrar logs
docker-compose logs gateway | grep "TAG DETECTADO"
```

### Detener Servicios
```bash
docker-compose down
```

### Detener y Eliminar Volúmenes (⚠️ Borra la base de datos)
```bash
docker-compose down -v
```

### Reiniciar un Servicio
```bash
docker-compose restart gateway
```

### Reconstruir Imagen
```bash
docker-compose build --no-cache gateway
```

## 🔍 Verificar que Funciona

### 1. Verificar que los Contenedores Estén Corriendo
```bash
docker-compose ps
```

Deberías ver:
```
NAME                    STATUS          PORTS
rfidgateway             Up (healthy)    0.0.0.0:8080->8080/tcp
rfidgateway-postgres    Up (healthy)    0.0.0.0:5432->5432/tcp
```

### 2. Verificar API
```bash
# Health check
curl http://localhost:8080/api/health

# Estado del sistema
curl http://localhost:8080/api/status
```

### 3. Verificar Logs
```bash
docker-compose logs gateway | tail -20
```

Deberías ver mensajes como:
```
Inicializando ReaderManager...
ReaderManager inicializado con X lectores
```

## 🗄️ Acceder a PostgreSQL

### Desde el Host
```bash
# Conectarse a PostgreSQL
docker exec -it rfidgateway-postgres psql -U rfiduser -d rfidgateway
```

### Desde Aplicación Externa
- **Host**: `localhost`
- **Puerto**: `5432`
- **Base de datos**: `rfidgateway`
- **Usuario**: `rfiduser`
- **Contraseña**: `changeme`

## ⚙️ Configuración de Lectores

Una vez que el gateway esté corriendo, configura los lectores en PostgreSQL:

```bash
# Conectarse a PostgreSQL
docker exec -it rfidgateway-postgres psql -U rfiduser -d rfidgateway

# Insertar lector
INSERT INTO readers (id, name, hostname, enabled) 
VALUES ('reader-1', 'Lector Entrada Principal', '192.168.1.100', true);

# Insertar antenas
INSERT INTO antennas (id, reader_id, name, port_number, enabled) 
VALUES 
  ('reader-1-antenna-1', 'reader-1', 'Antena Principal', 1, true),
  ('reader-1-antenna-2', 'reader-1', 'Antena Secundaria', 2, true);
```

**⚠️ IMPORTANTE**: Reemplaza `192.168.1.100` con la IP real de tu lector R220.

## 🔧 Variables de Entorno

Puedes modificar las variables de entorno en `docker-compose.yml`:

```yaml
environment:
  - DB_PASSWORD=tu_contraseña_segura
  - SPRING_PROFILES_ACTIVE=prod
```

O crear un archivo `.env`:

```env
DB_PASSWORD=tu_contraseña_segura
```

Y referenciarlo en `docker-compose.yml`:

```yaml
environment:
  - DB_PASSWORD=${DB_PASSWORD}
```

## 📊 Monitoreo

### Ver Uso de Recursos
```bash
docker stats
```

### Ver Procesos Dentro del Contenedor
```bash
docker exec -it rfidgateway ps aux
```

### Ver Variables de Entorno
```bash
docker exec rfidgateway env
```

## 🐛 Solución de Problemas

### El gateway no inicia

1. **Verificar logs**:
```bash
docker-compose logs gateway
```

2. **Verificar que PostgreSQL esté listo**:
```bash
docker-compose logs postgres
```

3. **Verificar conectividad**:
```bash
docker exec rfidgateway ping postgres
```

### Error de conexión a base de datos

1. **Verificar que PostgreSQL esté corriendo**:
```bash
docker-compose ps postgres
```

2. **Verificar credenciales** en `docker-compose.yml`

3. **Reiniciar servicios**:
```bash
docker-compose restart
```

### El gateway no detecta lectores

1. **Verificar configuración de lectores en BD**:
```bash
docker exec -it rfidgateway-postgres psql -U rfiduser -d rfidgateway -c "SELECT * FROM readers;"
```

2. **Verificar logs del gateway**:
   
   **PowerShell:**
   ```powershell
   docker-compose logs gateway | Select-String -Pattern "lector|reader|connect" -CaseSensitive:$false
   ```
   
   **Linux/Mac:**
   ```bash
   docker-compose logs gateway | grep -i "lector\|reader\|connect"
   ```

3. **Verificar que las IPs de los lectores sean accesibles desde el contenedor**

### Puerto 8080 ya está en uso

Modifica el puerto en `docker-compose.yml`:

```yaml
ports:
  - "8081:8080"  # Cambiar 8081 por el puerto que quieras
```

### Limpiar Todo y Empezar de Nuevo

```bash
# Detener y eliminar contenedores, redes y volúmenes
docker-compose down -v

# Eliminar imágenes
docker rmi rfidgateway_gateway

# Volver a construir e iniciar
docker-compose up -d --build
```

## 🔄 Actualizar el Gateway

```bash
# 1. Detener servicios
docker-compose down

# 2. Reconstruir imagen
docker-compose build --no-cache

# 3. Iniciar servicios
docker-compose up -d
```

## 📝 Persistencia de Datos

Los datos de PostgreSQL se guardan en un volumen de Docker:

```bash
# Ver volúmenes
docker volume ls

# Ver detalles del volumen
docker volume inspect rfidgateway_postgres_data

# Hacer backup
docker exec rfidgateway-postgres pg_dump -U rfiduser rfidgateway > backup.sql

# Restaurar backup
docker exec -i rfidgateway-postgres psql -U rfiduser rfidgateway < backup.sql
```

## 🌐 Acceso desde Red Local

Para acceder al gateway desde otros dispositivos en la red:

1. **Obtener IP del host**:
```bash
# Windows
ipconfig

# Linux/Mac
ifconfig
```

2. **Acceder desde otro dispositivo**:
```
http://[IP_DEL_HOST]:8080/api/status
```

## ✅ Checklist de Verificación

- [ ] Docker Desktop instalado y corriendo
- [ ] Contenedores iniciados (`docker-compose ps`)
- [ ] API responde (`curl http://localhost:8080/api/health`)
- [ ] PostgreSQL accesible
- [ ] Lectores configurados en BD
- [ ] Gateway conectado a lectores (ver logs)
- [ ] Eventos siendo detectados

---

**¡Listo! Tu gateway debería estar funcionando en Docker.**

