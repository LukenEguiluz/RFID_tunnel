# Plan de Implementación - V1: Sesiones RFID Controladas

## 📋 Resumen Ejecutivo

Este documento detalla el plan de implementación para la **Versión 1 (V1)** del sistema de sesiones RFID controladas. Esta versión permite iniciar sesiones de lectura bajo demanda desde una webapp, almacenar temporalmente los EPCs detectados durante la sesión, y consultarlos mediante polling.

---

## 🎯 Objetivos de la V1

### Funcionalidad Principal
- **Sesiones de lectura controladas**: Una sesión RFID por salida, iniciada mediante comando desde webapp
- **Almacenamiento temporal**: Gateway almacena lista de EPCs únicos detectados durante la sesión
- **Consulta mediante polling**: Frontend consulta cada 1 segundo el estado y EPCs de la sesión
- **Finalización automática**: La sesión se detiene automáticamente después de la lectura

### Limitaciones de la V1
- ❌ No usa WebSocket (solo polling HTTP)
- ❌ Una sesión activa por lector a la vez
- ❌ No persiste sesiones históricas en BD (solo en memoria)
- ❌ No permite configurar duración máxima de sesión
- ❌ No permite pausar/reanudar sesiones

---

## 🏗️ Arquitectura de la V1

### Flujo de Operación

```
┌─────────────┐
│   WebApp    │
│  (Frontend) │
└──────┬──────┘
       │
       │ 1. POST /api/sessions/start
       │    { "readerId": "reader-1" }
       │
       ▼
┌─────────────────────────────────┐
│      Gateway (Spring Boot)      │
│                                 │
│  ┌──────────────────────────┐  │
│  │  SessionController       │  │
│  │  - startSession()        │  │
│  │  - getSessionStatus()     │  │
│  │  - stopSession()         │  │
│  └───────────┬──────────────┘  │
│              │                  │
│  ┌───────────▼──────────────┐  │
│  │  SessionService          │  │
│  │  - Gestiona sesiones     │  │
│  │  - Almacena EPCs         │  │
│  └───────────┬──────────────┘  │
│              │                  │
│  ┌───────────▼──────────────┐  │
│  │  ReaderManager           │  │
│  │  - startSession()         │  │
│  │  - stopSession()          │  │
│  └───────────┬──────────────┘  │
│              │                  │
│  ┌───────────▼──────────────┐  │
│  │  SessionTagListener      │  │
│  │  - Captura EPCs          │  │
│  │  - Almacena en sesión    │  │
│  └──────────────────────────┘  │
└─────────────────────────────────┘
       │
       │ 2. Lectura RFID activa
       │
       ▼
┌─────────────┐
│  Reader R220│
│  (Hardware) │
└─────────────┘

       │
       │ 3. Polling cada 1 segundo
       │    GET /api/sessions/{sessionId}
       │
       ▼
┌─────────────┐
│   WebApp    │
│  (Frontend) │
└─────────────┘
```

### Componentes Nuevos

1. **SessionService**: Gestiona el ciclo de vida de las sesiones y almacena EPCs en memoria
2. **SessionController**: Endpoints REST para controlar sesiones
3. **SessionTagListener**: Listener especializado que captura EPCs solo durante sesiones activas
4. **Session Model**: Modelo de datos para representar una sesión (en memoria, no en BD)

---

## 📊 Modelo de Datos

### Entidad: Session (En Memoria)

```java
public class ReadingSession {
    private String sessionId;           // UUID único
    private String readerId;            // ID del lector
    private SessionStatus status;       // ACTIVE, STOPPED, COMPLETED
    private LocalDateTime startTime;    // Inicio de sesión
    private LocalDateTime endTime;      // Fin de sesión (null si activa)
    private Set<String> detectedEpcs;  // Set de EPCs únicos detectados
    private int totalReads;             // Total de lecturas (puede haber duplicados)
}
```

**Nota**: En V1, las sesiones se almacenan solo en memoria (ConcurrentHashMap). No se persisten en PostgreSQL.

### Estados de Sesión

- `ACTIVE`: Sesión iniciada, leyendo tags
- `STOPPED`: Sesión detenida manualmente
- `COMPLETED`: Sesión completada automáticamente

---

## 🔌 API REST - Nuevos Endpoints

### 1. Iniciar Sesión

```http
POST /api/sessions/start
Content-Type: application/json

{
  "readerId": "reader-1"
}
```

**Respuesta (201 Created)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "readerId": "reader-1",
  "status": "ACTIVE",
  "startTime": "2024-01-15T10:30:00.000Z",
  "message": "Session started successfully"
}
```

**Errores**:
- `400 Bad Request`: Lector no encontrado o ya tiene sesión activa
- `409 Conflict`: Ya existe una sesión activa para ese lector

### 2. Consultar Estado de Sesión (Polling)

```http
GET /api/sessions/{sessionId}
```

**Respuesta (200 OK)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "readerId": "reader-1",
  "status": "ACTIVE",
  "startTime": "2024-01-15T10:30:00.000Z",
  "endTime": null,
  "epcs": [
    "E200123456789012",
    "E200123456789013",
    "E200123456789014"
  ],
  "epcCount": 3,
  "totalReads": 15
}
```

**Errores**:
- `404 Not Found`: Sesión no encontrada

### 3. Detener Sesión

```http
POST /api/sessions/{sessionId}/stop
```

**Respuesta (200 OK)**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "STOPPED",
  "endTime": "2024-01-15T10:35:00.000Z",
  "epcs": [
    "E200123456789012",
    "E200123456789013"
  ],
  "epcCount": 2,
  "message": "Session stopped successfully"
}
```

### 4. Listar Sesiones Activas (Opcional para V1)

```http
GET /api/sessions/active
```

**Respuesta (200 OK)**:
```json
{
  "sessions": [
    {
      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
      "readerId": "reader-1",
      "status": "ACTIVE",
      "startTime": "2024-01-15T10:30:00.000Z",
      "epcCount": 3
    }
  ],
  "count": 1
}
```

---

## 🔧 Cambios en Componentes Existentes

### 1. ReaderManager

**Modificaciones necesarias**:
- Agregar método `startSession(String readerId)` que:
  - Verifica que no haya sesión activa
  - Inicia lectura solo si hay sesión activa
  - Configura listener especializado para sesiones
- Modificar `connectReader()` para NO iniciar lectura automáticamente
- Agregar método `stopSession(String readerId)` que detiene lectura y finaliza sesión
- Mantener estado de si el lector está en modo "sesión" o "continuo"

### 2. GatewayTagReportListener

**Modificaciones necesarias**:
- Agregar lógica condicional: si hay sesión activa, también almacenar EPC en la sesión
- Mantener compatibilidad con el flujo actual (guardar en BD siempre)

**Alternativa (Recomendada)**:
- Crear nuevo `SessionTagReportListener` específico para sesiones
- Usar este listener solo cuando hay sesión activa

### 3. TagEventService

**Sin cambios necesarios** en V1. El servicio actual sigue funcionando para guardar eventos en BD.

---

## 💾 Almacenamiento de Sesiones

### Estrategia: Memoria (ConcurrentHashMap)

```java
// En SessionService
private final Map<String, ReadingSession> activeSessions = new ConcurrentHashMap<>();
private final Map<String, ReadingSession> completedSessions = new ConcurrentHashMap<>();
```

**Consideraciones**:
- Sesiones activas: Se mantienen hasta que se detengan
- Sesiones completadas: Se mantienen en memoria por tiempo limitado (ej: 1 hora)
- Limpieza automática: Tarea programada que elimina sesiones antiguas

**Límites**:
- Máximo de sesiones activas: 1 por lector
- Máximo de sesiones completadas en memoria: 100 (configurable)

---

## 🔄 Flujo de Lectura en Sesión

### Inicio de Sesión

1. WebApp envía `POST /api/sessions/start` con `readerId`
2. `SessionService` crea nueva sesión con UUID
3. `SessionService` verifica que no haya sesión activa para ese lector
4. `ReaderManager.startSession()`:
   - Verifica que el lector esté conectado
   - Configura `SessionTagReportListener` en el lector
   - Inicia lectura con `reader.start()`
5. Retorna `sessionId` al cliente

### Durante la Sesión

1. `SessionTagReportListener.onTagReported()`:
   - Recibe tags del lector
   - Extrae EPCs
   - Agrega EPCs únicos al `Set<String>` de la sesión
   - Incrementa contador de lecturas totales
2. WebApp hace polling cada 1 segundo a `GET /api/sessions/{sessionId}`
3. Gateway retorna estado actualizado con lista de EPCs

### Finalización de Sesión

**Opción A: Manual (WebApp detiene)**
1. WebApp envía `POST /api/sessions/{sessionId}/stop`
2. `SessionService` marca sesión como `STOPPED`
3. `ReaderManager.stopSession()` detiene lectura
4. Retorna lista final de EPCs

**Opción B: Automática (Después de X tiempo sin nuevos tags)**
- **NO implementar en V1** (complejidad adicional)

**Opción C: Automática (Timeout fijo)**
- **NO implementar en V1** (complejidad adicional)

**Para V1**: Solo finalización manual.

---

## 📝 Tareas Técnicas - Orden de Implementación

### Fase 1: Modelo y Servicio Base

#### Tarea 1.1: Crear modelo `ReadingSession`
- **Archivo**: `src/main/java/com/rfidgateway/session/ReadingSession.java`
- **Descripción**: 
  - Clase POJO con campos: `sessionId`, `readerId`, `status`, `startTime`, `endTime`, `detectedEpcs` (Set<String>), `totalReads`
  - Enum `SessionStatus` con valores: `ACTIVE`, `STOPPED`, `COMPLETED`
  - Métodos: `addEpc(String epc)`, `stop()`, `complete()`
- **Dependencias**: Ninguna
- **Estimación**: 30 minutos

#### Tarea 1.2: Crear `SessionService`
- **Archivo**: `src/main/java/com/rfidgateway/session/SessionService.java`
- **Descripción**:
  - `@Service` de Spring
  - `ConcurrentHashMap<String, ReadingSession>` para sesiones activas
  - `ConcurrentHashMap<String, ReadingSession>` para sesiones completadas
  - Métodos:
    - `ReadingSession startSession(String readerId)`: Crea y retorna nueva sesión
    - `ReadingSession getSession(String sessionId)`: Obtiene sesión por ID
    - `ReadingSession stopSession(String sessionId)`: Detiene sesión
    - `List<ReadingSession> getActiveSessions()`: Lista sesiones activas
    - `boolean hasActiveSession(String readerId)`: Verifica si hay sesión activa
    - `void cleanupOldSessions()`: Limpia sesiones antiguas (tarea programada)
- **Dependencias**: Tarea 1.1
- **Estimación**: 1 hora

#### Tarea 1.3: Crear `SessionTagReportListener`
- **Archivo**: `src/main/java/com/rfidgateway/session/SessionTagReportListener.java`
- **Descripción**:
  - Implementa `TagReportListener` de Octane SDK
  - Constructor recibe: `sessionId`, `SessionService`
  - En `onTagReported()`: extrae EPCs y los agrega a la sesión usando `sessionService`
  - No guarda en BD (solo en sesión)
- **Dependencias**: Tarea 1.1, 1.2
- **Estimación**: 45 minutos

### Fase 2: Integración con ReaderManager

#### Tarea 2.1: Modificar `ReaderManager` para soportar sesiones
- **Archivo**: `src/main/java/com/rfidgateway/reader/ReaderManager.java`
- **Descripción**:
  - Inyectar `SessionService` como dependencia
  - Modificar `connectReader()`: NO iniciar lectura automáticamente (comentar `reader.start()`)
  - Agregar método `void startSession(String readerId, String sessionId)`:
    - Verifica que lector esté conectado
    - Configura `SessionTagReportListener` en el lector
    - Inicia lectura con `reader.start()`
  - Agregar método `void stopSession(String readerId)`:
    - Detiene lectura con `reader.stop()`
    - Remueve listener de sesión
  - Agregar método `boolean isInSessionMode(String readerId)`: verifica si está en modo sesión
- **Dependencias**: Tarea 1.2, 1.3
- **Estimación**: 1.5 horas

#### Tarea 2.2: Actualizar `GatewayTagReportListener` (Opcional)
- **Archivo**: `src/main/java/com/rfidgateway/reader/GatewayTagReportListener.java`
- **Descripción**:
  - **OPCIONAL**: Si queremos mantener lectura continua Y sesiones simultáneamente
  - Agregar lógica condicional para también agregar EPCs a sesión activa si existe
  - **DECISIÓN**: Para V1, usar listener separado (`SessionTagReportListener`) es más limpio
- **Dependencias**: Tarea 1.2
- **Estimación**: 30 minutos (si se implementa)

### Fase 3: API REST

#### Tarea 3.1: Crear `SessionController`
- **Archivo**: `src/main/java/com/rfidgateway/controller/SessionController.java`
- **Descripción**:
  - `@RestController` con `@RequestMapping("/api/sessions")`
  - Endpoints:
    - `POST /api/sessions/start`: Inicia sesión
      - Valida que no haya sesión activa para el lector
      - Crea sesión en `SessionService`
      - Llama a `ReaderManager.startSession()`
      - Retorna `sessionId` y estado
    - `GET /api/sessions/{sessionId}`: Consulta estado de sesión
      - Obtiene sesión de `SessionService`
      - Retorna estado, EPCs, contadores
    - `POST /api/sessions/{sessionId}/stop`: Detiene sesión
      - Llama a `SessionService.stopSession()`
      - Llama a `ReaderManager.stopSession()`
      - Retorna estado final
    - `GET /api/sessions/active`: Lista sesiones activas (opcional)
- **Dependencias**: Tarea 1.2, 2.1
- **Estimación**: 2 horas

#### Tarea 3.2: Crear DTOs de Request/Response
- **Archivos**: 
  - `src/main/java/com/rfidgateway/session/dto/StartSessionRequest.java`
  - `src/main/java/com/rfidgateway/session/dto/SessionResponse.java`
- **Descripción**:
  - DTOs para serialización JSON
  - Validaciones con `@Valid` y `@NotNull`
- **Dependencias**: Ninguna
- **Estimación**: 30 minutos

### Fase 4: Limpieza y Optimización

#### Tarea 4.1: Implementar limpieza automática de sesiones
- **Archivo**: `src/main/java/com/rfidgateway/session/SessionCleanupTask.java`
- **Descripción**:
  - `@Component` con `@Scheduled` (cada 10 minutos)
  - Elimina sesiones completadas mayores a 1 hora
  - Limita máximo de sesiones completadas en memoria (100)
- **Dependencias**: Tarea 1.2
- **Estimación**: 30 minutos

#### Tarea 4.2: Agregar logging y manejo de errores
- **Archivos**: Todos los nuevos archivos
- **Descripción**:
  - Agregar logs con `@Slf4j` en todos los componentes nuevos
  - Manejo de excepciones con try-catch apropiados
  - Mensajes de error descriptivos en respuestas HTTP
- **Dependencias**: Todas las tareas anteriores
- **Estimación**: 1 hora

### Fase 5: Testing y Validación

#### Tarea 5.1: Probar flujo completo manualmente
- **Descripción**:
  - Iniciar gateway
  - Verificar que lectores NO inicien lectura automáticamente
  - Iniciar sesión vía API
  - Verificar que se detecten tags y se agreguen a sesión
  - Hacer polling y verificar respuesta
  - Detener sesión y verificar estado final
- **Dependencias**: Todas las tareas anteriores
- **Estimación**: 1 hora

#### Tarea 5.2: Documentar API en README o archivo separado
- **Archivo**: `docs/API_SESIONES.md` (opcional)
- **Descripción**:
  - Documentar endpoints con ejemplos
  - Casos de uso
  - Códigos de error
- **Dependencias**: Tarea 3.1
- **Estimación**: 30 minutos

---

## 🔍 Consideraciones Técnicas

### 1. Modo Continuo vs Modo Sesión

**Problema**: El sistema actual inicia lectura continua automáticamente. Para V1, necesitamos que NO inicie automáticamente.

**Solución**:
- Modificar `ReaderManager.connectReader()` para NO llamar a `reader.start()` automáticamente
- Solo iniciar lectura cuando se cree una sesión
- Mantener flag `isInSessionMode` para saber si está en modo sesión

### 2. Listener de Tags

**Problema**: ¿Usar el listener actual o crear uno nuevo?

**Solución V1**:
- Crear `SessionTagReportListener` específico para sesiones
- Cuando se inicia sesión, reemplazar temporalmente el listener del lector
- Cuando se detiene sesión, restaurar listener original (o simplemente detener lectura)

**Alternativa** (más compleja):
- Modificar `GatewayTagReportListener` para que también agregue EPCs a sesión activa si existe
- Requiere inyectar `SessionService` en el listener

### 3. Almacenamiento de EPCs

**Problema**: ¿Cómo almacenar EPCs únicos eficientemente?

**Solución**:
- Usar `Set<String>` para EPCs únicos (automáticamente elimina duplicados)
- Mantener contador `totalReads` para estadísticas

### 4. Thread Safety

**Problema**: Múltiples threads pueden acceder a sesiones simultáneamente.

**Solución**:
- Usar `ConcurrentHashMap` para almacenar sesiones
- Usar `Collections.synchronizedSet()` o `ConcurrentHashMap.newKeySet()` para el Set de EPCs
- O usar `CopyOnWriteArraySet` (más seguro pero menos eficiente para escrituras frecuentes)

### 5. Limpieza de Sesiones

**Problema**: Sesiones completadas ocupan memoria indefinidamente.

**Solución**:
- Tarea programada que limpia sesiones mayores a 1 hora
- Límite máximo de sesiones completadas (ej: 100)
- Eliminar las más antiguas cuando se alcanza el límite

---

## ⚠️ Cambios Breaking (Compatibilidad)

### Cambio 1: Lectura Automática Deshabilitada

**Antes**: Los lectores iniciaban lectura automáticamente al conectar.

**Después**: Los lectores NO inician lectura automáticamente. Solo inician cuando se crea una sesión.

**Impacto**: 
- Cualquier sistema que dependa de lectura continua automática dejará de funcionar
- Se necesita migrar a usar sesiones o mantener ambos modos

**Solución Temporal**:
- Agregar flag de configuración `gateway.auto-start-reading` (default: false)
- Si es `true`, mantener comportamiento anterior
- Si es `false`, usar solo sesiones

### Cambio 2: API de Lectores

**Antes**: `POST /api/readers/{id}/start` iniciaba lectura continua.

**Después**: Este endpoint puede seguir funcionando, pero ahora inicia una sesión implícita o requiere crear sesión explícitamente.

**Recomendación**: 
- Mantener endpoints existentes para compatibilidad
- Documentar que ahora se recomienda usar `/api/sessions/start`

---

## 📋 Checklist de Implementación

### Pre-requisitos
- [ ] Revisar y entender código actual de `ReaderManager`
- [ ] Revisar y entender código actual de `GatewayTagReportListener`
- [ ] Verificar que el sistema actual funcione correctamente

### Fase 1: Modelo y Servicio Base
- [ ] Tarea 1.1: Crear modelo `ReadingSession`
- [ ] Tarea 1.2: Crear `SessionService`
- [ ] Tarea 1.3: Crear `SessionTagReportListener`

### Fase 2: Integración
- [ ] Tarea 2.1: Modificar `ReaderManager` para sesiones
- [ ] Tarea 2.2: (Opcional) Actualizar `GatewayTagReportListener`

### Fase 3: API REST
- [ ] Tarea 3.1: Crear `SessionController`
- [ ] Tarea 3.2: Crear DTOs

### Fase 4: Optimización
- [ ] Tarea 4.1: Implementar limpieza automática
- [ ] Tarea 4.2: Agregar logging y manejo de errores

### Fase 5: Testing
- [ ] Tarea 5.1: Probar flujo completo
- [ ] Tarea 5.2: Documentar API

### Post-Implementación
- [ ] Actualizar `README.md` con nueva funcionalidad
- [ ] Actualizar `ESPECIFICACION_TECNICA.md` si es necesario
- [ ] Crear ejemplos de uso de la API

---

## 🚀 Orden de Ejecución Recomendado

1. **Tarea 1.1** → Crear modelo base
2. **Tarea 1.2** → Crear servicio de sesiones
3. **Tarea 1.3** → Crear listener de sesiones
4. **Tarea 2.1** → Integrar con ReaderManager
5. **Tarea 3.2** → Crear DTOs
6. **Tarea 3.1** → Crear API REST
7. **Tarea 4.1** → Agregar limpieza automática
8. **Tarea 4.2** → Agregar logging
9. **Tarea 5.1** → Probar flujo completo
10. **Tarea 5.2** → Documentar

**Tiempo estimado total**: 8-10 horas de desarrollo

---

## 📝 Notas Adicionales

### Para Futuras Versiones (V2+)

- **Persistencia en BD**: Guardar sesiones en PostgreSQL
- **WebSocket**: Reemplazar polling por WebSocket para actualizaciones en tiempo real
- **Múltiples sesiones simultáneas**: Permitir sesiones en múltiples lectores a la vez
- **Timeout automático**: Finalizar sesión después de X segundos sin nuevos tags
- **Configuración de duración**: Permitir configurar duración máxima de sesión
- **Historial de sesiones**: Consultar sesiones históricas
- **Estadísticas**: Métricas por sesión (tiempo promedio de lectura, etc.)

### Decisiones de Diseño

1. **No persistir en BD en V1**: Simplifica implementación, reduce complejidad
2. **Polling en lugar de WebSocket**: Más simple para V1, menos overhead
3. **Una sesión por lector**: Evita conflictos y simplifica lógica
4. **Finalización manual**: Más control para el usuario, menos complejidad automática

---

## ✅ Criterios de Éxito de la V1

- [ ] WebApp puede iniciar sesión de lectura mediante API
- [ ] Gateway almacena EPCs únicos detectados durante la sesión
- [ ] WebApp puede consultar estado y EPCs mediante polling cada 1 segundo
- [ ] WebApp puede detener sesión y recibir lista final de EPCs
- [ ] Solo una sesión activa por lector a la vez
- [ ] Sesiones se limpian automáticamente después de 1 hora
- [ ] API retorna códigos HTTP apropiados y mensajes de error claros

---

**Documento creado**: 2024-01-15  
**Versión del plan**: 1.0  
**Estado**: Listo para implementación

