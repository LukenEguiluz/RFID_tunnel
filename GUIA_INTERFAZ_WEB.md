# 🌐 Guía de Interfaz Web - RFID Gateway

## 📋 Descripción

Se ha agregado una interfaz web sencilla (similar a Django) para gestionar lectores y antenas de forma visual, sin necesidad de usar SQL directamente.

## 🚀 Acceso

Una vez que el gateway esté corriendo, accede a:

**http://localhost:8080**

## ✨ Funcionalidades

### 1. Dashboard Principal (`/`)
- Vista general del estado del gateway
- Estadísticas: Total de lectores y lectores conectados
- Accesos rápidos a las funciones principales

### 2. Gestionar Lectores (`/readers`)
- Lista todos los lectores configurados
- Muestra estado de conexión y lectura
- Acciones disponibles:
  - ✏️ **Editar**: Modificar configuración del lector
  - 🔌 **Conectar**: Conectar manualmente al lector
  - 🔌 **Desconectar**: Desconectar el lector
  - 🗑️ **Eliminar**: Eliminar lector (con confirmación)

### 3. Agregar Lector (`/readers/new`)
Formulario simple para crear un nuevo lector:
- **ID del Lector**: Identificador único (sin espacios, ej: `reader-1`)
- **Nombre**: Nombre descriptivo (ej: `Lector Entrada Principal`)
- **Hostname/IP**: Dirección IP o hostname del lector R220 (ej: `192.168.1.100`)
- **Habilitado**: Si está marcado, se conectará automáticamente

### 4. Editar Lector (`/readers/{id}/edit`)
- Modificar nombre, hostname y estado habilitado
- **Gestionar Antenas**: Ver y agregar antenas para este lector
- Lista de antenas configuradas con sus parámetros

### 5. Agregar Antena (`/readers/{id}/antennas/new`)
Formulario para configurar una antena:
- **Nombre**: Nombre descriptivo (opcional)
- **Número de Puerto**: Puerto físico (1-4, típicamente 1 o 2 para R220)
- **Potencia de Transmisión (dBm)**: Opcional, dejar vacío para máxima
- **Sensibilidad de Recepción (dBm)**: Opcional, dejar vacío para máxima
- **Habilitada**: Si la antena está activa

## 🎨 Características de la Interfaz

- **Diseño Moderno**: Interfaz limpia y fácil de usar
- **Responsive**: Funciona en diferentes tamaños de pantalla
- **Mensajes de Confirmación**: Notificaciones de éxito/error
- **Confirmaciones**: Pregunta antes de eliminar
- **Estado Visual**: Indicadores de color para estados (conectado/desconectado/leyendo)

## 📝 Flujo de Trabajo Típico

### Configuración Inicial

1. **Acceder al Dashboard**: `http://localhost:8080`
2. **Agregar Primer Lector**:
   - Click en "➕ Agregar Lector"
   - Completar formulario:
     - ID: `reader-1`
     - Nombre: `Lector Entrada Principal`
     - Hostname: `192.168.1.100`
     - Habilitado: ✓
   - Click en "💾 Guardar"
3. **Configurar Antenas**:
   - Click en "✏️ Editar" del lector creado
   - Click en "➕ Agregar Antena"
   - Completar:
     - Puerto: `1`
     - Nombre: `Antena Principal`
     - Habilitada: ✓
   - Click en "💾 Guardar"
4. **Conectar**:
   - En la lista de lectores, click en "🔌 Conectar"
   - El lector debería conectarse automáticamente

### Gestión Diaria

- **Ver Estado**: Dashboard muestra estado general
- **Conectar/Desconectar**: Botones en la lista de lectores
- **Modificar Configuración**: Editar lectores y antenas según necesidad

## 🔧 Notas Técnicas

- La interfaz usa **Thymeleaf** (equivalente a Django templates en Spring)
- Los datos se guardan en **PostgreSQL** automáticamente
- Los cambios se aplican inmediatamente al guardar
- La conexión automática ocurre si el lector está habilitado

## 🐛 Solución de Problemas

### La interfaz no carga
- Verificar que el gateway esté corriendo: `docker-compose ps`
- Ver logs: `docker-compose logs -f gateway`
- Verificar puerto: `http://localhost:8080`

### Los lectores no se conectan
- Verificar que la IP/hostname sea correcta
- Verificar conectividad de red
- Revisar logs del gateway para errores específicos

### No se guardan los cambios
- Verificar conexión a PostgreSQL
- Revisar logs para errores de base de datos

## 📚 API REST Sigue Disponible

La interfaz web es complementaria a la API REST. Todas las funciones siguen disponibles vía API:

- `GET /api/readers` - Listar lectores
- `POST /api/readers` - Crear lector
- `GET /api/readers/{id}` - Obtener lector
- `POST /api/readers/{id}/start` - Iniciar lectura
- `POST /api/readers/{id}/stop` - Detener lectura
- `GET /api/events` - Obtener eventos de tags

## 🎯 Próximos Pasos

1. Agregar más lectores según tu instalación
2. Configurar antenas para cada lector
3. Conectar los lectores
4. Monitorear eventos de tags vía API o WebSocket





