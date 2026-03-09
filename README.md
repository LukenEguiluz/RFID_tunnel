# RFID Gateway - Gateway Centralizado para Lectores Impinj R220

Gateway centralizado desarrollado en Spring Boot para controlar y gestionar múltiples lectores RFID Impinj R220 y sus antenas.

## 🚀 Características

- ✅ Gestión centralizada de múltiples lectores R220
- ✅ Control de antenas con nombres personalizados
- ✅ Inventario continuo 24/7
- ✅ API REST completa (JSON)
- ✅ WebSocket para eventos en tiempo real
- ✅ Persistencia en PostgreSQL
- ✅ Reconexión automática
- ✅ Notificaciones de errores de conexión
- ✅ Docker para fácil despliegue

## 📋 Requisitos

- Java 11 o superior
- Maven 3.6+
- PostgreSQL 12+ (o usar Docker Compose)
- Docker y Docker Compose (opcional, recomendado)

## 🛠️ Instalación

### Opción 1: Docker Compose (Recomendado)

1. **Clonar o copiar el proyecto**

2. **Construir la aplicación**:
```bash
mvn clean package
```

3. **Iniciar con Docker Compose**:
```bash
docker-compose up -d
```

El gateway estará disponible en `http://localhost:8080`

### Opción 2: Instalación Manual

1. **Configurar PostgreSQL**:
```sql
CREATE DATABASE rfidgateway;
CREATE USER rfiduser WITH PASSWORD 'changeme';
GRANT ALL PRIVILEGES ON DATABASE rfidgateway TO rfiduser;
```

2. **Configurar variables de entorno**:
```bash
export DB_PASSWORD=changeme
```

3. **Compilar y ejecutar**:
```bash
mvn clean package
java -jar target/rfid-gateway-1.0.0.jar
```

## ⚙️ Configuración de Lectores

Los lectores se configuran en la base de datos PostgreSQL. Puedes insertarlos directamente o usar la API REST.

### Insertar Lectores Manualmente

```sql
-- Insertar lector
INSERT INTO readers (id, name, hostname, enabled) 
VALUES ('reader-1', 'Lector Entrada Principal', '192.168.1.100', true);

-- Insertar antenas del lector
INSERT INTO antennas (id, reader_id, name, port_number, enabled) 
VALUES 
  ('reader-1-antenna-1', 'reader-1', 'Antena Principal', 1, true),
  ('reader-1-antenna-2', 'reader-1', 'Antena Secundaria', 2, true);
```

### Usar API REST (cuando el gateway esté corriendo)

```bash
# Agregar lector (ejemplo con curl)
curl -X POST http://localhost:8080/api/readers \
  -H "Content-Type: application/json" \
  -d '{
    "id": "reader-1",
    "name": "Lector Entrada Principal",
    "hostname": "192.168.1.100",
    "enabled": true
  }'
```

## 📡 API REST

### Lectores

- `GET /api/readers` - Listar todos los lectores
- `GET /api/readers/{id}` - Información de un lector
- `GET /api/readers/{id}/status` - Estado de un lector
- `POST /api/readers/{id}/start` - Iniciar lectura
- `POST /api/readers/{id}/stop` - Detener lectura

### Eventos

- `GET /api/events` - Listar eventos (con paginación y filtros)
  - Parámetros: `epc`, `reader`, `antenna`, `from`, `to`, `page`, `size`
- `GET /api/events/latest?limit=20` - Últimos eventos

### Grupos de lectores

- `GET /api/groups` - **Listar todos los grupos**
- `GET /api/groups/{id}` - Obtener un grupo por ID
- `POST /api/groups` - Crear grupo (body: id, name, description, readerIds, enabled)
- `PUT /api/groups/{id}` - Actualizar grupo
- `DELETE /api/groups/{id}` - Eliminar grupo
- `GET /api/groups/{id}/stats` - Estadísticas del grupo

Para iniciar lectura en todos los lectores de un grupo: `POST /api/sessions/start` con `{"groupId": "id-del-grupo"}`. Documentación completa: **[API_GRUPOS_LECTORES.md](API_GRUPOS_LECTORES.md)**.

### Estado

- `GET /api/status` - Estado general del gateway
- `GET /api/health` - Health check

### Ejemplos

```bash
# Listar lectores
curl http://localhost:8080/api/readers

# Listar grupos de lectores
curl http://localhost:8080/api/groups

# Obtener últimos 20 eventos
curl http://localhost:8080/api/events/latest?limit=20

# Eventos de un EPC específico
curl http://localhost:8080/api/events?epc=E200123456789012

# Eventos de un lector
curl http://localhost:8080/api/events?reader=reader-1

# Estado del sistema
curl http://localhost:8080/api/status
```

## 🔌 WebSocket

Conectarse a `ws://localhost:8080/ws/events` para recibir eventos en tiempo real.

### Eventos WebSocket

```json
// Tag detectado
{
  "type": "TAG_DETECTED",
  "timestamp": "2024-01-15T10:30:45.123Z",
  "data": {
    "epc": "E200123456789012",
    "readerId": "reader-1",
    "antennaId": "reader-1-antenna-1",
    "antennaPort": 1,
    "rssi": -65.5,
    "phase": 1.23
  }
}

// Lector desconectado
{
  "type": "READER_DISCONNECTED",
  "timestamp": "2024-01-15T10:30:45.123Z",
  "data": {
    "readerId": "reader-1",
    "readerName": "Lector Entrada Principal",
    "reason": "Connection lost"
  }
}

// Lector reconectado
{
  "type": "READER_RECONNECTED",
  "timestamp": "2024-01-15T10:30:50.456Z",
  "data": {
    "readerId": "reader-1",
    "readerName": "Lector Entrada Principal"
  }
}
```

### Ejemplo de Cliente WebSocket (JavaScript)

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/events');

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Evento recibido:', data);
  
  if (data.type === 'TAG_DETECTED') {
    console.log('Tag detectado:', data.data.epc);
  }
};

ws.onopen = () => {
  console.log('Conectado al WebSocket');
};
```

## 🗄️ Base de Datos

### Esquema

- **readers**: Información de lectores
- **antennas**: Configuración de antenas
- **tag_events**: Eventos de tags detectados

### Recomendación de Retención

Para mantener el rendimiento, se recomienda:

1. **Mantener eventos por 1 año** (configurable)
2. **Crear particiones mensuales** para mejor rendimiento
3. **Script de limpieza automática** (opcional)

Ejemplo de script de limpieza:

```sql
-- Eliminar eventos mayores a 1 año
DELETE FROM tag_events 
WHERE detected_at < NOW() - INTERVAL '1 year';
```

## 🔧 Configuración

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rfidgateway
    username: rfiduser
    password: ${DB_PASSWORD}

server:
  port: 8080
```

### Variables de Entorno

- `DB_PASSWORD`: Contraseña de PostgreSQL
- `DB_HOST`: Host de PostgreSQL (default: localhost)
- `DB_PORT`: Puerto de PostgreSQL (default: 5432)
- `DB_NAME`: Nombre de la base de datos (default: rfidgateway)
- `DB_USER`: Usuario de PostgreSQL (default: rfiduser)

## 🐛 Solución de Problemas

### El lector no se conecta

1. Verificar que el lector esté en la misma red
2. Verificar que el hostname/IP sea correcto
3. Verificar que el lector esté encendido
4. Revisar logs: `docker-compose logs gateway`

### No se detectan tags

1. Verificar que el lector esté en modo "reading"
2. Verificar configuración de antenas
3. Verificar que las antenas estén habilitadas
4. Revisar logs para errores

### Base de datos no conecta

1. Verificar que PostgreSQL esté corriendo
2. Verificar credenciales en `application.yml`
3. Verificar que la base de datos exista

## 📝 Logs

Los logs se muestran en la consola. Para Docker:

```bash
# Ver logs del gateway
docker-compose logs -f gateway

# Ver logs de PostgreSQL
docker-compose logs -f postgres
```

## 🔒 Seguridad

- Por defecto, la API no tiene autenticación (según requerimientos)
- Para producción, se recomienda:
  - Agregar autenticación JWT
  - Usar HTTPS/TLS
  - Configurar firewall
  - Cambiar contraseñas por defecto

## 📚 Documentación Adicional

- [Especificación Técnica](./ESPECIFICACION_TECNICA.md)
- [Capacidades del Octane SDK](./CAPACIDADES_OCTANE_SDK.md)

## 🤝 Contribuir

Este es un proyecto personalizado. Para modificaciones, contactar al desarrollador.

## 📄 Licencia

Este proyecto utiliza el SDK de Impinj que tiene su propia licencia. Ver archivos de licencia en `Octane_SDK_Java_3_0_0/`.

---

**Desarrollado para control de inventarios con lectores Impinj R220**







