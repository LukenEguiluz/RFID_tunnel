# Especificación Técnica - Gateway RFID

## 📊 Resumen de Requerimientos

### Infraestructura
- **Lectores**: 2 inicialmente (escalable a muchos)
- **Antenas**: 2 por R220
- **Red**: Local (misma red)
- **Acceso**: Local y remoto (webapp puede leer o recibir push)

### Funcionalidad
- **Caso de uso**: Control de inventarios, registro de salidas/entradas de maletas
- **Modo**: Inventario continuo 24/7
- **Eventos**: Solo detección de tags (EPCs)
- **Operaciones**: No escritura frecuente

### Tecnología
- **Lenguaje**: Java 11+
- **Framework**: Spring Boot (fácil mantenimiento)
- **Base de datos**: PostgreSQL
- **Formato**: JSON
- **Despliegue**: Docker
- **Plataforma**: Windows y Linux

### Características Especiales
- ✅ Lectores con nombres personalizados
- ✅ Reconexión automática
- ✅ Notificación de errores de conexión
- ✅ Persistencia de eventos (recomendación: limpieza periódica)
- ✅ API REST + WebSocket
- ✅ Configuración estática similar para todos

---

## 🏗️ Arquitectura Final

```
┌─────────────────────────────────────────────────────────────┐
│                    GATEWAY RFID (Spring Boot)                │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  REST API    │  │  WebSocket   │  │  Event       │      │
│  │  (JSON)      │  │  (Real-time) │  │  Processor   │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         └──────────────────┴──────────────────┘              │
│                          │                                   │
│              ┌───────────▼───────────┐                      │
│              │  Reader Manager      │                      │
│              │  (Named Readers)      │                      │
│              └───────────┬───────────┘                      │
│                          │                                   │
│              ┌───────────▼───────────┐                      │
│              │  Tag Event Service    │                      │
│              │  (Deduplication)      │                      │
│              └───────────┬───────────┘                      │
│                          │                                   │
│              ┌───────────▼───────────┐                      │
│              │  PostgreSQL            │                      │
│              │  (Event Storage)       │                      │
│              └────────────────────────┘                      │
└──────────────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐       ┌─────▼─────┐      ┌─────▼─────┐
   │ Reader 1│       │ Reader 2  │      │ Reader N  │
   │ (Nombre)│       │ (Nombre)   │      │ (Nombre)  │
   │ R220    │       │ R220       │      │ R220      │
   │         │       │            │      │           │
   │ Ant 1-2 │       │ Ant 1-2    │      │ Ant 1-2   │
   └─────────┘       └────────────┘      └───────────┘
```

---

## 📡 API REST Endpoints

### Lectores
```
GET    /api/readers                    # Listar todos los lectores
GET    /api/readers/{id}               # Info de un lector específico
GET    /api/readers/{id}/status        # Estado de conexión
POST   /api/readers/{id}/start         # Iniciar inventario
POST   /api/readers/{id}/stop          # Detener inventario
```

### Antenas
```
GET    /api/antennas                   # Listar todas las antenas
GET    /api/antennas/{id}              # Info de antena específica
GET    /api/readers/{readerId}/antennas # Antenas de un lector
```

### Eventos/Tags
```
GET    /api/events                     # Eventos históricos (con paginación)
GET    /api/events/latest              # Últimos eventos
GET    /api/events?epc={epc}          # Eventos de un EPC específico
GET    /api/events?reader={readerId}   # Eventos de un lector
GET    /api/events?antenna={antennaId}  # Eventos de una antena
GET    /api/events?from={timestamp}&to={timestamp} # Rango de tiempo
```

### Estado del Sistema
```
GET    /api/status                     # Estado general del gateway
GET    /api/health                     # Health check
GET    /api/metrics                    # Métricas (tags/min, lectores activos, etc.)
```

---

## 🔌 WebSocket Events

### Eventos que se envían al cliente:

```json
// Tag detectado
{
  "type": "TAG_DETECTED",
  "timestamp": "2024-01-15T10:30:45.123Z",
  "data": {
    "epc": "E200123456789012",
    "readerId": "reader-1",
    "readerName": "Lector Entrada",
    "antennaId": "reader-1-antenna-1",
    "antennaName": "Antena Principal",
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
    "readerName": "Lector Entrada",
    "reason": "Connection lost"
  }
}

// Lector reconectado
{
  "type": "READER_RECONNECTED",
  "timestamp": "2024-01-15T10:30:50.456Z",
  "data": {
    "readerId": "reader-1",
    "readerName": "Lector Entrada"
  }
}

// Error
{
  "type": "ERROR",
  "timestamp": "2024-01-15T10:30:45.123Z",
  "data": {
    "readerId": "reader-1",
    "readerName": "Lector Entrada",
    "error": "Failed to start reader",
    "message": "Connection timeout"
  }
}
```

---

## 🗄️ Esquema de Base de Datos

### Tabla: `readers`
```sql
CREATE TABLE readers (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    hostname VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Tabla: `antennas`
```sql
CREATE TABLE antennas (
    id VARCHAR(50) PRIMARY KEY,
    reader_id VARCHAR(50) NOT NULL,
    name VARCHAR(100),
    port_number SMALLINT NOT NULL,
    enabled BOOLEAN DEFAULT true,
    tx_power_dbm DECIMAL(5,2),
    rx_sensitivity_dbm DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reader_id) REFERENCES readers(id)
);
```

### Tabla: `tag_events`
```sql
CREATE TABLE tag_events (
    id BIGSERIAL PRIMARY KEY,
    epc VARCHAR(96) NOT NULL,
    reader_id VARCHAR(50) NOT NULL,
    antenna_id VARCHAR(50) NOT NULL,
    antenna_port SMALLINT NOT NULL,
    rssi DECIMAL(5,2),
    phase DECIMAL(8,4),
    detected_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_epc (epc),
    INDEX idx_reader (reader_id),
    INDEX idx_antenna (antenna_id),
    INDEX idx_detected_at (detected_at)
);
```

**Recomendación de retención**: 
- Mantener eventos por 1 año
- Crear particiones mensuales para mejor rendimiento
- Script de limpieza automática opcional

---

## ⚙️ Configuración

### Archivo: `application.yml`
```yaml
gateway:
  readers:
    - id: reader-1
      name: "Lector Entrada Principal"
      hostname: 192.168.1.100
      enabled: true
    - id: reader-2
      name: "Lector Almacén"
      hostname: 192.168.1.101
      enabled: true
  
  auto-reconnect:
    enabled: true
    interval-seconds: 30
    max-retries: -1  # infinito
  
  inventory:
    mode: CONTINUOUS
    session: 1
    reader-mode: AutoSetDenseReader
    search-mode: SingleTarget

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rfidgateway
    username: rfiduser
    password: ${DB_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

server:
  port: 8080

logging:
  level:
    com.impinj: INFO
    com.rfidgateway: INFO
```

---

## 🐳 Docker

### docker-compose.yml
```yaml
version: '3.8'

services:
  gateway:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DB_PASSWORD=changeme
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - postgres
    volumes:
      - ./config:/app/config
    restart: unless-stopped

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=rfidgateway
      - POSTGRES_USER=rfiduser
      - POSTGRES_PASSWORD=changeme
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    restart: unless-stopped

volumes:
  postgres_data:
```

---

## 📦 Estructura del Proyecto

```
RFIDgateway/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/rfidgateway/
│       │       ├── GatewayApplication.java
│       │       ├── config/
│       │       │   ├── ReaderConfig.java
│       │       │   └── WebSocketConfig.java
│       │       ├── reader/
│       │       │   ├── ReaderManager.java
│       │       │   ├── ReaderService.java
│       │       │   └── ReaderConnectionListener.java
│       │       ├── antenna/
│       │       │   └── AntennaService.java
│       │       ├── tag/
│       │       │   ├── TagEventService.java
│       │       │   └── TagEventProcessor.java
│       │       ├── model/
│       │       │   ├── Reader.java
│       │       │   ├── Antenna.java
│       │       │   └── TagEvent.java
│       │       ├── repository/
│       │       │   ├── ReaderRepository.java
│       │       │   ├── AntennaRepository.java
│       │       │   └── TagEventRepository.java
│       │       ├── controller/
│       │       │   ├── ReaderController.java
│       │       │   ├── AntennaController.java
│       │       │   ├── EventController.java
│       │       │   └── StatusController.java
│       │       └── websocket/
│       │           └── EventWebSocketHandler.java
│       └── resources/
│           ├── application.yml
│           └── application-prod.yml
├── config/
│   └── readers.json
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── pom.xml
├── README.md
└── .gitignore
```

---

## 🚀 Plan de Implementación

### Fase 1: Core y Conexión (Prioridad Alta)
1. ✅ Estructura del proyecto Spring Boot
2. ✅ Configuración de lectores con nombres
3. ✅ ReaderManager con conexión a múltiples lectores
4. ✅ Reconexión automática
5. ✅ Listener de eventos de tags

### Fase 2: Persistencia y API (Prioridad Alta)
1. ✅ Modelos de datos (JPA)
2. ✅ Repositorios
3. ✅ Servicio de eventos de tags
4. ✅ API REST básica
5. ✅ Guardado de eventos en PostgreSQL

### Fase 3: WebSocket y Procesamiento (Prioridad Media)
1. ✅ WebSocket para eventos en tiempo real
2. ✅ Deduplicación de tags
3. ✅ Filtrado y procesamiento

### Fase 4: Monitoreo y Optimización (Prioridad Baja)
1. ✅ Health checks
2. ✅ Métricas
3. ✅ Logging estructurado
4. ✅ Optimizaciones de rendimiento

---

## ✅ Listo para Implementar

Con esta especificación, procederé a construir el gateway completo.

