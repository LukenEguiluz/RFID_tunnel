# Arquitectura Actual del Sistema RFID Gateway

## 📋 Índice

1. [Visión General](#visión-general)
2. [Arquitectura de Alto Nivel](#arquitectura-de-alto-nivel)
3. [Componentes Principales](#componentes-principales)
4. [Flujo de Datos](#flujo-de-datos)
5. [Modelos de Datos](#modelos-de-datos)
6. [API REST](#api-rest)
7. [WebSocket y Tiempo Real](#websocket-y-tiempo-real)
8. [Base de Datos](#base-de-datos)
9. [Configuración](#configuración)
10. [Ciclo de Vida y Inicialización](#ciclo-de-vida-y-inicialización)

---

## 🎯 Visión General

El **RFID Gateway** es una aplicación Spring Boot que actúa como intermediario centralizado entre múltiples lectores RFID Impinj R220 y aplicaciones cliente (webapps). Su función principal es:

- **Gestionar conexiones** a múltiples lectores RFID simultáneamente
- **Capturar eventos** de tags detectados en tiempo real
- **Persistir eventos** en base de datos PostgreSQL
- **Exponer API REST** para consulta y control
- **Proporcionar WebSocket** para eventos en tiempo real
- **Reconexión automática** en caso de pérdida de conexión

### Características Principales

- ✅ **Lectura continua 24/7**: Los lectores inician lectura automáticamente al conectar
- ✅ **Persistencia completa**: Todos los eventos se guardan en PostgreSQL
- ✅ **Múltiples lectores**: Soporta N lectores simultáneamente
- ✅ **Múltiples antenas**: Cada lector puede tener hasta 4 antenas (configurables)
- ✅ **Reconexión automática**: Intenta reconectar cada 30 segundos si se pierde conexión
- ✅ **WebSocket y SSE**: Dos mecanismos para eventos en tiempo real
- ✅ **API REST completa**: Control y consulta de lectores, antenas y eventos

---

## 🏗️ Arquitectura de Alto Nivel

```
┌─────────────────────────────────────────────────────────────────────┐
│                    APLICACIONES CLIENTE                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐            │
│  │   WebApp 1   │  │   WebApp 2   │  │  Dashboard    │            │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘            │
│         │                  │                  │                      │
│         └──────────────────┴──────────────────┘                      │
│                            │                                         │
│         ┌───────────────────▼───────────────────┐                   │
│         │      HTTP/REST API (Puerto 8080)       │                   │
│         │  - GET /api/readers                    │                   │
│         │  - POST /api/readers/{id}/start        │                   │
│         │  - GET /api/events                     │                   │
│         └───────────────────┬───────────────────┘                   │
│                              │                                       │
│         ┌───────────────────▼───────────────────┐                   │
│         │      WebSocket (ws://:8080/ws/events)   │                   │
│         │      SSE (/api/realtime/events)         │                   │
│         └───────────────────┬───────────────────┘                   │
└──────────────────────────────┼───────────────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────────────┐
│                    GATEWAY RFID (Spring Boot)                         │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    CAPA DE CONTROL                          │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │    │
│  │  │ReaderController│ │EventController│ │StatusController│   │    │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │    │
│  │         │                  │                  │            │    │
│  └─────────┼──────────────────┼──────────────────┼────────────┘    │
│            │                  │                  │                   │
│  ┌─────────▼──────────────────▼──────────────────▼────────────┐    │
│  │                    CAPA DE SERVICIOS                       │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │    │
│  │  │ReaderManager │  │TagEventService│ │WebSocketService│   │    │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │    │
│  └─────────┼──────────────────┼──────────────────┼────────────┘    │
│            │                  │                  │                   │
│  ┌─────────▼──────────────────▼──────────────────▼────────────┐    │
│  │                    CAPA DE LISTENERS                        │    │
│  │  ┌──────────────┐  ┌──────────────┐                       │    │
│  │  │GatewayTag    │  │GatewayConn    │                       │    │
│  │  │ReportListener│  │LostListener   │                       │    │
│  │  └──────┬───────┘  └──────┬───────┘                       │    │
│  └─────────┼──────────────────┼──────────────────────────────┘    │
│            │                  │                                     │
│  ┌─────────▼──────────────────▼──────────────────────────────┐    │
│  │                    CAPA DE PERSISTENCIA                     │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │    │
│  │  │ReaderRepo    │  │TagEventRepo   │  │AntennaRepo    │   │    │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │    │
│  └─────────┼──────────────────┼──────────────────┼────────────┘    │
│            │                  │                  │                   │
└────────────┼──────────────────┼──────────────────┼───────────────────┘
             │                  │                  │
             ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    POSTGRESQL (Puerto 5432)                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │   readers    │  │ tag_events   │  │  antennas    │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└─────────────────────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    LECTORES RFID (Hardware)                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ Reader 1     │  │ Reader 2     │  │ Reader N     │           │
│  │ (R220)       │  │ (R220)        │  │ (R220)       │           │
│  │ IP: 192.168  │  │ IP: 192.168   │  │ IP: 192.168   │           │
│  │ .1.100       │  │ .1.101        │  │ .1.10N       │           │
│  │              │  │               │  │              │           │
│  │ Ant 1, Ant 2 │  │ Ant 1, Ant 2 │  │ Ant 1-4      │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 Componentes Principales

### 1. GatewayApplication

**Ubicación**: `com.rfidgateway.GatewayApplication`

**Responsabilidades**:
- Punto de entrada de la aplicación Spring Boot
- Habilita procesamiento asíncrono (`@EnableAsync`)
- Habilita tareas programadas (`@EnableScheduling`)

**Código Clave**:
```java
@SpringBootApplication(exclude = {GsonAutoConfiguration.class})
@EnableAsync
@EnableScheduling
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

---

### 2. ReaderManager

**Ubicación**: `com.rfidgateway.reader.ReaderManager`

**Responsabilidades**:
- **Gestión centralizada** de todos los lectores RFID
- **Conexión automática** a lectores habilitados al iniciar
- **Configuración** de lectores (modo, sesión, antenas)
- **Control de lectura** (start/stop)
- **Reconexión automática** en caso de pérdida de conexión
- **Gestión de listeners** (tag reports, connection lost)

**Estructura Interna**:
```java
@Component
public class ReaderManager {
    // Almacenamiento en memoria
    private final Map<String, ImpinjReader> readers = new ConcurrentHashMap<>();
    private final Map<String, ReaderInfo> readerInfos = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reconnectExecutor = Executors.newScheduledThreadPool(5);
    
    // Dependencias inyectadas
    @Autowired private ReaderRepository readerRepository;
    @Autowired private AntennaRepository antennaRepository;
    @Autowired private TagEventService tagEventService;
    @Autowired private WebSocketEventService webSocketEventService;
}
```

**Flujo de Inicialización**:
1. `@PostConstruct initialize()`: Se ejecuta al iniciar la aplicación
2. Consulta `readerRepository.findByEnabledTrue()` para obtener lectores habilitados
3. Para cada lector, llama a `connectReader(Reader readerConfig)`
4. `connectReader()` realiza:
   - Crea instancia `ImpinjReader` del SDK
   - Conecta al lector físico vía IP
   - Configura settings (modo, sesión, antenas)
   - Configura listeners (`GatewayTagReportListener`, `GatewayConnectionLostListener`)
   - Aplica configuración con `applySettings()`
   - **Inicia lectura automáticamente** con `reader.start()`
   - Actualiza estado en BD (`isConnected=true`, `isReading=true`)
   - Notifica reconexión vía WebSocket

**Métodos Principales**:

- `connectReader(Reader readerConfig)`: Conecta y configura un lector
- `disconnectReader(String readerId)`: Desconecta un lector
- `startReader(String readerId)`: Inicia lectura en un lector
- `stopReader(String readerId)`: Detiene lectura en un lector
- `resetReader(String readerId)`: Reinicia conexión (desconecta y reconecta)
- `rebootReader(String readerId)`: Reboot completo (espera 5 segundos)
- `resetAntennas(String readerId)`: Reinicia solo configuración de antenas
- `handleConnectionLost(String readerId)`: Maneja pérdida de conexión
- `scheduleReconnect(String readerId, Reader config)`: Programa reconexión automática

**Configuración de Lectores**:
El método `configureReaderSettings()` configura:
- **ReaderMode**: `AutoSetDenseReader` (modo denso automático)
- **SearchMode**: `SingleTarget` (un tag a la vez)
- **Session**: `1` (sesión 1 para inventario)
- **ReportConfig**: 
  - `Mode.Individual`: Reporta cada tag individualmente
  - `IncludeAntennaPortNumber`: Incluye puerto de antena
  - `IncludePeakRssi`: Incluye RSSI
  - `IncludeLastSeenTime`: Incluye timestamp
  - `IncludeSeenCount`: Incluye contador de veces visto
- **Antennas**: Habilita solo las antenas configuradas en BD y habilitadas

**Reconexión Automática**:
- Si se pierde conexión, `handleConnectionLost()` se ejecuta
- Programa reconexión después de 30 segundos
- Intenta reconectar indefinidamente (`max-retries: -1`)
- Notifica vía WebSocket cuando se reconecta

---

### 3. GatewayTagReportListener

**Ubicación**: `com.rfidgateway.reader.GatewayTagReportListener`

**Responsabilidades**:
- **Implementa** `TagReportListener` del Octane SDK
- **Recibe** reportes de tags del lector físico
- **Extrae** información de cada tag (EPC, antena, RSSI, fase)
- **Delega** procesamiento a `TagEventService`

**Flujo de Ejecución**:
```java
@Override
public void onTagReported(ImpinjReader reader, TagReport report) {
    List<Tag> tags = report.getTags();
    
    for (Tag tag : tags) {
        String epc = tag.getEpc().toHexString();
        Short antennaPort = tag.getAntennaPortNumber();
        Double rssi = tag.getPeakRssiInDbm();
        Double phase = tag.getPhaseAngleInRadians();
        
        // Procesar evento
        tagEventService.processTagEvent(
            readerId, epc, antennaPort, rssi, phase
        );
    }
}
```

**Características**:
- Se crea una instancia por lector (recibe `readerId` en constructor)
- Se registra en el lector durante `connectReader()`
- Se ejecuta en thread del SDK (no bloquea operaciones principales)
- Logs cada tag detectado con información completa

---

### 4. GatewayConnectionLostListener

**Ubicación**: `com.rfidgateway.reader.GatewayConnectionLostListener`

**Responsabilidades**:
- **Implementa** `ConnectionLostListener` del Octane SDK
- **Detecta** cuando se pierde conexión con un lector
- **Notifica** al `ReaderManager` para iniciar reconexión

**Flujo de Ejecución**:
```java
@Override
public void onConnectionLost(ImpinjReader reader) {
    log.warn("Conexión perdida con lector {}", readerId);
    readerManager.handleConnectionLost(readerId);
}
```

**Características**:
- Se crea una instancia por lector
- Se registra durante `connectReader()`
- Permite reconexión automática sin intervención manual

---

### 5. TagEventService

**Ubicación**: `com.rfidgateway.tag.TagEventService`

**Responsabilidades**:
- **Procesa** eventos de tags detectados
- **Resuelve** ID de antena desde BD
- **Persiste** eventos en PostgreSQL
- **Notifica** vía WebSocket y SSE

**Flujo de Procesamiento**:
```java
@Transactional
public void processTagEvent(String readerId, String epc, Short antennaPort, 
                            Double rssi, Double phase) {
    // 1. Obtener ID de antena desde BD
    String antennaId = antennaRepository
        .findByReaderIdAndPortNumber(readerId, antennaPort)
        .map(antenna -> antenna.getId())
        .orElse(readerId + "-antenna-" + antennaPort);
    
    // 2. Crear entidad TagEvent
    TagEvent event = new TagEvent();
    event.setEpc(epc);
    event.setReaderId(readerId);
    event.setAntennaId(antennaId);
    event.setAntennaPort(antennaPort);
    event.setRssi(rssi);
    event.setPhase(phase);
    event.setDetectedAt(LocalDateTime.now());
    
    // 3. Guardar en BD
    tagEventRepository.save(event);
    
    // 4. Notificar vía WebSocket
    webSocketEventService.notifyTagDetected(...);
    
    // 5. Notificar vía SSE (opcional)
    if (realtimeEventController != null) {
        realtimeEventController.broadcastEvent(event);
    }
}
```

**Características**:
- `@Transactional`: Garantiza consistencia de datos
- Manejo de errores: Captura excepciones y las loguea sin interrumpir flujo
- Resolución de antena: Si no encuentra en BD, genera ID por defecto

---

### 6. WebSocketEventService

**Ubicación**: `com.rfidgateway.tag.WebSocketEventService`

**Responsabilidades**:
- **Abstracción** sobre el handler de WebSocket
- **Notifica** eventos de tags detectados
- **Notifica** eventos de conexión/desconexión de lectores

**Métodos**:
- `notifyTagDetected(...)`: Notifica tag detectado
- `notifyReaderDisconnected(...)`: Notifica lector desconectado
- `notifyReaderReconnected(...)`: Notifica lector reconectado

**Características**:
- `@Autowired(required = false)`: No falla si WebSocket no está disponible
- Delega en `EventWebSocketHandler` para envío real

---

### 7. EventWebSocketHandler

**Ubicación**: `com.rfidgateway.websocket.EventWebSocketHandler`

**Responsabilidades**:
- **Maneja** conexiones WebSocket
- **Mantiene** registro de clientes conectados
- **Envía** eventos a todos los clientes conectados

**Estructura**:
```java
@Component
public class EventWebSocketHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
}
```

**Ciclo de Vida**:
- `afterConnectionEstablished()`: Se ejecuta cuando cliente se conecta
- `afterConnectionClosed()`: Se ejecuta cuando cliente se desconecta
- `sendTagDetectedEvent()`: Envía evento de tag detectado
- `sendReaderDisconnectedEvent()`: Envía evento de desconexión
- `sendReaderReconnectedEvent()`: Envía evento de reconexión

**Formato de Mensajes**:
```json
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
```

**Características**:
- Broadcast a todos los clientes conectados
- Manejo de errores: Si falla envío a un cliente, no afecta a otros
- Thread-safe: Usa `ConcurrentHashMap` para sesiones

---

### 8. Controllers (API REST)

#### 8.1 ReaderController

**Ubicación**: `com.rfidgateway.controller.ReaderController`

**Endpoints**:
- `GET /api/readers`: Lista todos los lectores
- `GET /api/readers/{id}`: Obtiene información de un lector
- `GET /api/readers/{id}/status`: Obtiene estado de un lector
- `POST /api/readers/{id}/start`: Inicia lectura en un lector
- `POST /api/readers/{id}/stop`: Detiene lectura en un lector
- `POST /api/readers/{id}/reset`: Reinicia conexión de un lector
- `POST /api/readers/{id}/reboot`: Reboot completo de un lector
- `POST /api/readers/{id}/antennas/reset`: Reinicia configuración de antenas

**Características**:
- Valida existencia de lector antes de operar
- Valida que lector esté habilitado antes de iniciar
- Retorna códigos HTTP apropiados (200, 404, 400)

#### 8.2 EventController

**Ubicación**: `com.rfidgateway.controller.EventController`

**Endpoints**:
- `GET /api/events`: Lista eventos con paginación y filtros
  - Parámetros: `epc`, `reader`, `antenna`, `from`, `to`, `page`, `size`
- `GET /api/events/latest?limit=20`: Obtiene últimos N eventos

**Filtros Soportados**:
- Por EPC: `?epc=E200123456789012`
- Por lector: `?reader=reader-1`
- Por antena: `?antenna=reader-1-antenna-1`
- Por rango de tiempo: `?from=2024-01-15T10:00:00&to=2024-01-15T11:00:00`
- Paginación: `?page=0&size=50`

**Respuesta**:
```json
{
  "events": [...],
  "totalElements": 1000,
  "totalPages": 20,
  "currentPage": 0,
  "size": 50
}
```

#### 8.3 AntennaController

**Ubicación**: `com.rfidgateway.controller.AntennaController`

**Endpoints**:
- `GET /api/antennas`: Lista todas las antenas
- `GET /api/antennas/{id}`: Obtiene información de una antena
- `GET /api/antennas/reader/{readerId}`: Lista antenas de un lector

#### 8.4 StatusController

**Ubicación**: `com.rfidgateway.controller.StatusController`

**Endpoints**:
- `GET /api/status`: Estado general del gateway
  - Retorna: total de lectores, conectados, leyendo, lista de lectores
- `GET /api/health`: Health check simple
  - Retorna: `{"status": "UP"}`

#### 8.5 RealtimeEventController

**Ubicación**: `com.rfidgateway.controller.RealtimeEventController`

**Endpoints**:
- `GET /api/realtime/events`: Stream SSE (Server-Sent Events)
  - Parámetros opcionales: `readerId`, `epc`
  - Envía eventos en tiempo real vía SSE
  - Envía últimos 10 eventos al conectar
- `GET /api/realtime/events/latest`: Últimos eventos (HTTP)
- `GET /api/realtime/stats`: Estadísticas en tiempo real
  - Tags detectados en último minuto
  - Tags detectados en última hora
  - Total de eventos

**Características SSE**:
- Mantiene lista de `SseEmitter` activos
- Envía eventos a todos los clientes conectados
- Limpia emisores cuando se desconectan
- Timeout: `Long.MAX_VALUE` (sin timeout)

---

## 🔄 Flujo de Datos

### Flujo Principal: Detección de Tag

```
1. Lector RFID (Hardware)
   │
   │ Detecta tag físico
   │
   ▼
2. Octane SDK (ImpinjReader)
   │
   │ Genera TagReport
   │
   ▼
3. GatewayTagReportListener.onTagReported()
   │
   │ Extrae: EPC, antena, RSSI, fase
   │
   ▼
4. TagEventService.processTagEvent()
   │
   │ Resuelve ID de antena desde BD
   │
   ▼
5. TagEventRepository.save()
   │
   │ Persiste en PostgreSQL
   │
   ▼
6. WebSocketEventService.notifyTagDetected()
   │
   │ Notifica vía WebSocket
   │
   ▼
7. EventWebSocketHandler.sendTagDetectedEvent()
   │
   │ Envía a todos los clientes WebSocket conectados
   │
   ▼
8. Clientes WebSocket reciben evento en tiempo real
```

### Flujo de Inicialización

```
1. Spring Boot inicia aplicación
   │
   ▼
2. GatewayApplication.main()
   │
   │ Carga configuración Spring
   │
   ▼
3. ReaderManager.initialize() (@PostConstruct)
   │
   │ Consulta lectores habilitados en BD
   │
   ▼
4. Para cada lector habilitado:
   │
   ├─► ReaderManager.connectReader()
   │   │
   │   ├─► Crea ImpinjReader
   │   ├─► Conecta vía IP
   │   ├─► Configura settings
   │   ├─► Configura listeners
   │   ├─► Aplica configuración
   │   └─► Inicia lectura (reader.start())
   │
   ▼
5. Lectores activos y leyendo tags
```

### Flujo de Reconexión Automática

```
1. Pérdida de conexión detectada
   │
   ▼
2. GatewayConnectionLostListener.onConnectionLost()
   │
   │ Notifica a ReaderManager
   │
   ▼
3. ReaderManager.handleConnectionLost()
   │
   │ Actualiza estado en BD (isConnected=false)
   │ Notifica vía WebSocket
   │
   ▼
4. ReaderManager.scheduleReconnect()
   │
   │ Programa tarea después de 30 segundos
   │
   ▼
5. Tarea ejecuta después de 30 segundos
   │
   │ Intenta reconectar
   │
   ▼
6. ReaderManager.connectReader()
   │
   │ Reconecta y reinicia lectura
   │
   ▼
7. Notifica reconexión vía WebSocket
```

---

## 📊 Modelos de Datos

### 1. Reader

**Ubicación**: `com.rfidgateway.model.Reader`

**Tabla**: `readers`

**Campos**:
```java
@Id
private String id;                    // ID único del lector

@Column(nullable = false, unique = true)
private String name;                  // Nombre descriptivo

@Column(nullable = false)
private String hostname;              // IP o hostname del lector

@Column(nullable = false)
private Boolean enabled = true;        // Si está habilitado

@Column(name = "is_connected")
private Boolean isConnected = false;   // Estado de conexión

@Column(name = "is_reading")
private Boolean isReading = false;    // Si está leyendo tags

@Column(name = "last_seen")
private LocalDateTime lastSeen;       // Última vez visto

@Column(name = "created_at", updatable = false)
private LocalDateTime createdAt;      // Fecha de creación

@Column(name = "updated_at")
private LocalDateTime updatedAt;      // Fecha de última actualización
```

**Índices**: Ninguno (tabla pequeña)

**Relaciones**: 
- Uno a muchos con `Antenna` (cascade delete)

---

### 2. Antenna

**Ubicación**: `com.rfidgateway.model.Antenna`

**Tabla**: `antennas`

**Campos**:
```java
@Id
private String id;                    // ID único de la antena

@Column(name = "reader_id", nullable = false)
private String readerId;              // FK a readers.id

@Column
private String name;                  // Nombre descriptivo

@Column(name = "port_number", nullable = false)
private Short portNumber;             // Puerto físico (1-4)

@Column(nullable = false)
private Boolean enabled = true;       // Si está habilitada

@Column(name = "tx_power_dbm")
private Double txPowerDbm;            // Potencia de transmisión (dBm)

@Column(name = "rx_sensitivity_dbm")
private Double rxSensitivityDbm;      // Sensibilidad de recepción (dBm)

@Column(name = "created_at", updatable = false)
private LocalDateTime createdAt;      // Fecha de creación

@Column(name = "updated_at")
private LocalDateTime updatedAt;      // Fecha de última actualización
```

**Índices**: Ninguno explícito (FK tiene índice automático)

**Relaciones**:
- Muchos a uno con `Reader` (FK `reader_id`)

---

### 3. TagEvent

**Ubicación**: `com.rfidgateway.model.TagEvent`

**Tabla**: `tag_events`

**Campos**:
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;                      // ID auto-incremental

@Column(nullable = false, length = 96)
private String epc;                  // EPC del tag (hex string)

@Column(name = "reader_id", nullable = false)
private String readerId;              // FK a readers.id

@Column(name = "antenna_id", nullable = false)
private String antennaId;            // FK a antennas.id

@Column(name = "antenna_port", nullable = false)
private Short antennaPort;            // Puerto de antena (1-4)

@Column
private Double rssi;                 // RSSI en dBm

@Column
private Double phase;                // Fase en radianes

@Column(name = "detected_at", nullable = false)
private LocalDateTime detectedAt;     // Timestamp de detección

@Column(name = "created_at", updatable = false)
private LocalDateTime createdAt;      // Fecha de creación en BD
```

**Índices**:
- `idx_epc`: Sobre `epc` (búsquedas por EPC)
- `idx_reader`: Sobre `reader_id` (búsquedas por lector)
- `idx_antenna`: Sobre `antenna_id` (búsquedas por antena)
- `idx_detected_at`: Sobre `detected_at` (búsquedas por tiempo)

**Relaciones**:
- Muchos a uno con `Reader` (FK `reader_id`)
- Muchos a uno con `Antenna` (FK `antenna_id`)

**Características**:
- Tabla de alto volumen (puede tener millones de registros)
- Se recomienda limpieza periódica (ej: eventos > 1 año)
- Índices optimizan consultas frecuentes

---

## 🔌 API REST

### Base URL
```
http://localhost:8080/api
```

### Endpoints por Recurso

#### Lectores

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/readers` | Lista todos los lectores |
| GET | `/readers/{id}` | Obtiene un lector específico |
| GET | `/readers/{id}/status` | Estado de un lector |
| POST | `/readers/{id}/start` | Inicia lectura |
| POST | `/readers/{id}/stop` | Detiene lectura |
| POST | `/readers/{id}/reset` | Reinicia conexión |
| POST | `/readers/{id}/reboot` | Reboot completo |
| POST | `/readers/{id}/antennas/reset` | Reinicia antenas |

#### Eventos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/events` | Lista eventos (paginado, filtros) |
| GET | `/events/latest?limit=N` | Últimos N eventos |

**Filtros de `/events`**:
- `epc`: Filtrar por EPC
- `reader`: Filtrar por lector
- `antenna`: Filtrar por antena
- `from`: Fecha inicio (ISO 8601)
- `to`: Fecha fin (ISO 8601)
- `page`: Número de página (default: 0)
- `size`: Tamaño de página (default: 50)

#### Antenas

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/antennas` | Lista todas las antenas |
| GET | `/antennas/{id}` | Obtiene una antena |
| GET | `/antennas/reader/{readerId}` | Antenas de un lector |

#### Estado

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/status` | Estado general del gateway |
| GET | `/health` | Health check |

#### Tiempo Real

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/realtime/events` | Stream SSE de eventos |
| GET | `/realtime/events/latest` | Últimos eventos (HTTP) |
| GET | `/realtime/stats` | Estadísticas en tiempo real |

---

## 🌐 WebSocket y Tiempo Real

### WebSocket

**Endpoint**: `ws://localhost:8080/ws/events`

**Configuración**: `WebSocketConfig`
- Handler: `EventWebSocketHandler`
- Ruta: `/ws/events`
- Orígenes permitidos: `*` (todos)

**Tipos de Eventos**:

1. **TAG_DETECTED**: Tag detectado
```json
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
```

2. **READER_DISCONNECTED**: Lector desconectado
```json
{
  "type": "READER_DISCONNECTED",
  "timestamp": "2024-01-15T10:30:45.123Z",
  "data": {
    "readerId": "reader-1",
    "readerName": "Lector Entrada",
    "reason": "Connection lost"
  }
}
```

3. **READER_RECONNECTED**: Lector reconectado
```json
{
  "type": "READER_RECONNECTED",
  "timestamp": "2024-01-15T10:30:50.456Z",
  "data": {
    "readerId": "reader-1",
    "readerName": "Lector Entrada"
  }
}
```

**Ejemplo de Cliente JavaScript**:
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/events');

ws.onopen = () => {
    console.log('Conectado al WebSocket');
};

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log('Evento recibido:', data);
    
    if (data.type === 'TAG_DETECTED') {
        console.log('Tag detectado:', data.data.epc);
    }
};

ws.onerror = (error) => {
    console.error('Error WebSocket:', error);
};

ws.onclose = () => {
    console.log('Conexión WebSocket cerrada');
};
```

### Server-Sent Events (SSE)

**Endpoint**: `GET /api/realtime/events`

**Características**:
- Stream continuo de eventos
- Envía últimos 10 eventos al conectar
- Parámetros opcionales: `readerId`, `epc`
- Timeout: Sin límite (`Long.MAX_VALUE`)

**Ejemplo de Cliente JavaScript**:
```javascript
const eventSource = new EventSource('http://localhost:8080/api/realtime/events');

eventSource.addEventListener('tag', (event) => {
    const data = JSON.parse(event.data);
    console.log('Tag detectado:', data);
});

eventSource.addEventListener('connected', (event) => {
    console.log('Conectado al stream SSE');
});

eventSource.onerror = (error) => {
    console.error('Error SSE:', error);
};
```

---

## 🗄️ Base de Datos

### Esquema Completo

```sql
-- Tabla: readers
CREATE TABLE readers (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    hostname VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    is_connected BOOLEAN DEFAULT false,
    is_reading BOOLEAN DEFAULT false,
    last_seen TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: antennas
CREATE TABLE antennas (
    id VARCHAR(50) PRIMARY KEY,
    reader_id VARCHAR(50) NOT NULL,
    name VARCHAR(100),
    port_number SMALLINT NOT NULL,
    enabled BOOLEAN DEFAULT true,
    tx_power_dbm DECIMAL(5,2),
    rx_sensitivity_dbm DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reader_id) REFERENCES readers(id) ON DELETE CASCADE
);

-- Tabla: tag_events
CREATE TABLE tag_events (
    id BIGSERIAL PRIMARY KEY,
    epc VARCHAR(96) NOT NULL,
    reader_id VARCHAR(50) NOT NULL,
    antenna_id VARCHAR(50) NOT NULL,
    antenna_port SMALLINT NOT NULL,
    rssi DECIMAL(5,2),
    phase DECIMAL(8,4),
    detected_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices
CREATE INDEX idx_epc ON tag_events(epc);
CREATE INDEX idx_reader ON tag_events(reader_id);
CREATE INDEX idx_antenna ON tag_events(antenna_id);
CREATE INDEX idx_detected_at ON tag_events(detected_at);
```

### Configuración JPA

- **DDL Auto**: `update` (crea/actualiza tablas automáticamente)
- **Dialect**: PostgreSQL
- **Show SQL**: `false` (en producción)

### Conexión

- **URL**: `jdbc:postgresql://localhost:5432/rfidgateway`
- **Usuario**: `rfiduser`
- **Contraseña**: Variable de entorno `DB_PASSWORD` (default: `changeme`)

---

## ⚙️ Configuración

### application.yml

```yaml
spring:
  application:
    name: rfid-gateway
  
  datasource:
    url: jdbc:postgresql://localhost:5432/rfidgateway
    username: rfiduser
    password: ${DB_PASSWORD:changeme}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  jackson:
    serialization:
      write-dates-as-timestamps: false
    time-zone: UTC

server:
  port: 8080

logging:
  level:
    root: INFO
    com.rfidgateway: DEBUG
    com.rfidgateway.reader: INFO
    com.impinj: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %-5level [%logger{36}] - %msg%n"

gateway:
  auto-reconnect:
    enabled: true
    interval-seconds: 30
    max-retries: -1  # -1 = infinito
```

### Variables de Entorno

- `DB_PASSWORD`: Contraseña de PostgreSQL (default: `changeme`)
- `SPRING_PROFILES_ACTIVE`: Perfil activo (default: sin perfil)

---

## 🔄 Ciclo de Vida y Inicialización

### 1. Inicio de Aplicación

```
1. Spring Boot inicia
   │
   ▼
2. Carga application.yml
   │
   ▼
3. Inicializa DataSource (PostgreSQL)
   │
   ▼
4. Hibernate crea/actualiza tablas
   │
   ▼
5. Crea beans de Spring:
   - ReaderRepository
   - AntennaRepository
   - TagEventRepository
   - ReaderManager
   - TagEventService
   - WebSocketEventService
   - EventWebSocketHandler
   - Controllers
   │
   ▼
6. ReaderManager.initialize() (@PostConstruct)
   │
   ▼
7. Conecta a lectores habilitados
   │
   ▼
8. Aplicación lista para recibir requests
```

### 2. Conexión de Lector

```
1. ReaderManager.connectReader(Reader config)
   │
   ├─► Crea ImpinjReader
   ├─► impinjReader.connect(hostname)
   ├─► impinjReader.stop() (reset)
   ├─► settings = impinjReader.queryDefaultSettings()
   ├─► configureReaderSettings(settings)
   ├─► Configura listeners
   ├─► impinjReader.applySettings(settings)
   ├─► Thread.sleep(1000) (pausa)
   ├─► impinjReader.start() (inicia lectura)
   ├─► Actualiza BD (isConnected=true, isReading=true)
   └─► Notifica vía WebSocket
```

### 3. Detección de Tag

```
1. Lector físico detecta tag
   │
   ▼
2. Octane SDK genera TagReport
   │
   ▼
3. GatewayTagReportListener.onTagReported()
   │
   ├─► Extrae EPC, antena, RSSI, fase
   └─► tagEventService.processTagEvent(...)
       │
       ├─► Resuelve antennaId desde BD
       ├─► Crea TagEvent
       ├─► tagEventRepository.save() (persiste)
       ├─► webSocketEventService.notifyTagDetected()
       └─► realtimeEventController.broadcastEvent() (SSE)
```

### 4. Pérdida de Conexión

```
1. Conexión se pierde
   │
   ▼
2. GatewayConnectionLostListener.onConnectionLost()
   │
   ▼
3. ReaderManager.handleConnectionLost()
   │
   ├─► Actualiza BD (isConnected=false, isReading=false)
   ├─► Notifica vía WebSocket
   └─► scheduleReconnect() (después de 30 segundos)
       │
       ▼
4. Tarea ejecuta después de 30 segundos
   │
   └─► connectReader() (reintenta conexión)
```

### 5. Shutdown

```
1. Aplicación recibe señal de shutdown
   │
   ▼
2. ReaderManager.shutdown() (@PreDestroy)
   │
   ├─► Cierra ScheduledExecutorService
   ├─► Para cada lector:
   │   ├─► reader.stop()
   │   └─► reader.disconnect()
   └─► Limpia maps en memoria
   │
   ▼
3. Spring cierra contexto
   │
   ▼
4. Aplicación termina
```

---

## 📝 Resumen de Características Actuales

### ✅ Funcionalidades Implementadas

1. **Gestión de Lectores**
   - Conexión automática al iniciar
   - Lectura continua 24/7
   - Control manual (start/stop/reset/reboot)
   - Reconexión automática

2. **Gestión de Antenas**
   - Configuración por antena
   - Potencia y sensibilidad configurables
   - Habilitación/deshabilitación individual

3. **Captura de Eventos**
   - Detección en tiempo real
   - Persistencia completa en BD
   - Información completa (EPC, antena, RSSI, fase, timestamp)

4. **API REST**
   - CRUD de lectores y antenas
   - Consulta de eventos con filtros
   - Control de lectores
   - Estado del sistema

5. **Tiempo Real**
   - WebSocket para eventos en vivo
   - SSE para streaming
   - Notificaciones de conexión/desconexión

6. **Persistencia**
   - PostgreSQL con JPA/Hibernate
   - Índices optimizados
   - Relaciones FK con cascade

### ⚠️ Limitaciones Actuales

1. **Lectura Continua Siempre**
   - Los lectores inician lectura automáticamente
   - No hay modo "sesión" controlada
   - No se puede iniciar/detener lectura bajo demanda fácilmente

2. **Persistencia Completa**
   - Todos los eventos se guardan (puede crecer mucho)
   - No hay limpieza automática
   - No hay particionamiento

3. **Sin Autenticación**
   - API abierta sin autenticación
   - WebSocket sin autenticación

4. **Una Configuración por Lector**
   - No hay múltiples perfiles de configuración
   - Configuración estática en BD

---

**Documento creado**: 2024-01-15  
**Versión**: 1.0  
**Estado**: Documentación completa de arquitectura actual

