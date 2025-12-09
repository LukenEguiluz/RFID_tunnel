# 👥 API de Grupos de Lectores

Esta API permite agrupar múltiples lectores RFID y controlarlos como una unidad. Al iniciar una sesión con un `groupId`, todos los lectores del grupo inician lectura simultáneamente.

## 🎯 Concepto

En lugar de enviar `readerId` individual, ahora puedes enviar `groupId`:

```json
{
  "groupId": "entrada-principal"
}
```

Esto iniciará lectura en **todos los lectores** del grupo simultáneamente.

## 🔌 Endpoints de Grupos

### 1. Crear Grupo

```http
POST /api/groups
Content-Type: application/json

{
  "id": "entrada-principal",
  "name": "Entrada Principal",
  "description": "Lectores en la entrada principal del edificio",
  "readerIds": ["reader-1", "reader-2", "reader-3"],
  "enabled": true
}
```

**Respuesta (201 Created)**:
```json
{
  "id": "entrada-principal",
  "name": "Entrada Principal",
  "description": "Lectores en la entrada principal del edificio",
  "readers": [
    { "id": "reader-1", "name": "Lector 1", ... },
    { "id": "reader-2", "name": "Lector 2", ... },
    { "id": "reader-3", "name": "Lector 3", ... }
  ],
  "enabled": true
}
```

### 2. Listar Grupos

```http
GET /api/groups
```

**Respuesta**:
```json
[
  {
    "id": "entrada-principal",
    "name": "Entrada Principal",
    "readers": [...],
    "enabled": true
  },
  {
    "id": "salida-secundaria",
    "name": "Salida Secundaria",
    "readers": [...],
    "enabled": true
  }
]
```

### 3. Obtener Grupo por ID

```http
GET /api/groups/{groupId}
```

### 4. Actualizar Grupo

```http
PUT /api/groups/{groupId}
Content-Type: application/json

{
  "name": "Nuevo Nombre",
  "readerIds": ["reader-1", "reader-4"],
  "enabled": true
}
```

### 5. Eliminar Grupo

```http
DELETE /api/groups/{groupId}
```

### 6. Estadísticas del Grupo

```http
GET /api/groups/{groupId}/stats
```

**Respuesta**:
```json
{
  "groupId": "entrada-principal",
  "groupName": "Entrada Principal",
  "totalReaders": 3,
  "enabledReaders": 3,
  "connectedReaders": 2
}
```

---

## 📡 Iniciar Sesión con Grupo

### Endpoint Modificado

El endpoint `/api/sessions/start` ahora acepta **tanto `groupId` como `readerId`**:

**Con grupo (nuevo)**:
```http
POST /api/sessions/start
Content-Type: application/json

{
  "groupId": "entrada-principal"
}
```

**Con lector individual (legacy, sigue funcionando)**:
```http
POST /api/sessions/start
Content-Type: application/json

{
  "readerId": "reader-1"
}
```

### Respuesta de Sesión de Grupo

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "groupId": "entrada-principal",
  "readerIds": ["reader-1", "reader-2", "reader-3"],
  "readerCount": 3,
  "status": "ACTIVE",
  "startTime": "2024-01-15T10:30:00.000",
  "message": "Sesión de grupo iniciada exitosamente"
}
```

### Consultar Sesión de Grupo

```http
GET /api/sessions/{sessionId}
```

**Respuesta**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "groupId": "entrada-principal",
  "readerIds": ["reader-1", "reader-2", "reader-3"],
  "status": "ACTIVE",
  "epcs": [
    "E200123456789012",
    "E200123456789013"
  ],
  "epcCount": 2,
  "totalReads": 15
}
```

**Nota**: Los `epcs` son la **unión de todos los tags detectados por todos los lectores del grupo**.

---

## 📋 Ejemplos de Uso

### Ejemplo 1: Crear Grupo y Usarlo

```bash
# 1. Crear grupo
curl -X POST http://localhost:8080/api/groups \
  -H "Content-Type: application/json" \
  -d '{
    "id": "entrada-principal",
    "name": "Entrada Principal",
    "description": "Lectores en entrada",
    "readerIds": ["reader-1", "reader-2"],
    "enabled": true
  }'

# 2. Iniciar sesión con el grupo
curl -X POST http://localhost:8080/api/sessions/start \
  -H "Content-Type: application/json" \
  -d '{"groupId": "entrada-principal"}'

# 3. Consultar estado (polling)
curl http://localhost:8080/api/sessions/{sessionId}

# 4. Detener sesión
curl -X POST http://localhost:8080/api/sessions/{sessionId}/stop
```

### Ejemplo 2: JavaScript

```javascript
// Crear grupo
async function createGroup(groupId, name, readerIds) {
  const response = await fetch('http://localhost:8080/api/groups', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      id: groupId,
      name: name,
      readerIds: readerIds,
      enabled: true
    })
  });
  return await response.json();
}

// Iniciar sesión con grupo
async function startGroupSession(groupId) {
  const response = await fetch('http://localhost:8080/api/sessions/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ groupId })
  });
  return await response.json();
}

// Uso
const group = await createGroup('entrada-principal', 'Entrada Principal', 
  ['reader-1', 'reader-2']);
const session = await startGroupSession('entrada-principal');
console.log('Sesión iniciada:', session.sessionId);
```

### Ejemplo 3: PowerShell

```powershell
# Crear grupo
$groupBody = @{
    id = "entrada-principal"
    name = "Entrada Principal"
    description = "Lectores en entrada"
    readerIds = @("reader-1", "reader-2")
    enabled = $true
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/groups" `
  -Method POST `
  -ContentType "application/json" `
  -Body $groupBody

# Iniciar sesión
$sessionBody = @{
    groupId = "entrada-principal"
} | ConvertTo-Json

$session = Invoke-RestMethod -Uri "http://localhost:8080/api/sessions/start" `
  -Method POST `
  -ContentType "application/json" `
  -Body $sessionBody

Write-Host "Sesión iniciada: $($session.sessionId)"
```

---

## ⚠️ Notas Importantes

1. **Lectores conectados**: Solo los lectores **conectados y habilitados** del grupo iniciarán lectura
2. **Tags combinados**: Los tags detectados por todos los lectores del grupo se combinan en una sola lista
3. **Una sesión por grupo**: Solo puede haber una sesión activa por grupo a la vez
4. **Compatibilidad**: El sistema sigue soportando `readerId` para sesiones individuales

---

## 🔍 Casos de Uso

### Caso 1: Entrada con Múltiples Lectores
```json
{
  "id": "entrada-principal",
  "name": "Entrada Principal",
  "readerIds": ["reader-entrada-1", "reader-entrada-2", "reader-entrada-3"]
}
```
Todos los lectores leen simultáneamente para cubrir toda el área de entrada.

### Caso 2: Zona Completa
```json
{
  "id": "zona-almacen",
  "name": "Zona de Almacén",
  "readerIds": ["reader-almacen-norte", "reader-almacen-sur", "reader-almacen-este", "reader-almacen-oeste"]
}
```
Cubre toda una zona con múltiples lectores.

### Caso 3: Pasillo con Múltiples Puntos
```json
{
  "id": "pasillo-principal",
  "name": "Pasillo Principal",
  "readerIds": ["reader-pasillo-1", "reader-pasillo-2", "reader-pasillo-3"]
}
```
Rastrea movimiento a lo largo de un pasillo.

---

## 📊 Ventajas de Usar Grupos

1. **Simplicidad**: Un solo comando para múltiples lectores
2. **Sincronización**: Todos los lectores inician/detienen simultáneamente
3. **Tags combinados**: Una sola lista con todos los tags detectados
4. **Gestión centralizada**: Fácil agregar/quitar lectores del grupo

---

## 🔄 Migración desde readerId

Si ya tienes código que usa `readerId`, puedes:

1. **Mantenerlo**: Sigue funcionando igual
2. **Migrar a grupos**: Crea grupos y usa `groupId` en su lugar
3. **Ambos**: Puedes usar ambos según necesites

