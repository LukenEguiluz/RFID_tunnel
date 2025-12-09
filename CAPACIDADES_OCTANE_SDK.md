# Guía Completa de Capacidades del Octane SDK para Impinj R220

## Resumen Ejecutivo

El **Octane SDK Java 3.0.0** es una biblioteca completa para controlar y gestionar lectores RFID Impinj, incluyendo los modelos R220. Este SDK proporciona acceso a todas las funcionalidades del lector a través de una API orientada a objetos en Java.

---

## 1. CONEXIÓN Y GESTIÓN DE LECTORES

### 1.1 Conexión Básica
- **Conexión a un lector**: `reader.connect(hostname)`
- **Conexión asíncrona**: Soporte para conexiones asíncronas con callbacks
- **Desconexión**: `reader.disconnect()`
- **Múltiples lectores**: Puedes conectar y gestionar varios lectores simultáneamente

### 1.2 Consulta de Información del Lector
- **Características del lector**: `queryFeatureSet()` - Obtiene modelo, firmware, número de antenas
- **Configuración actual**: `querySettings()` - Obtiene toda la configuración actual
- **Configuración por defecto**: `queryDefaultSettings()` - Obtiene configuración predeterminada
- **Estado del lector**: `queryStatus()` - Estado de conexión, temperatura, si está leyendo, etc.

### 1.3 Gestión de Configuración
- **Aplicar configuración**: `applySettings(settings)`
- **Guardar configuración**: `settings.save("archivo.xml")` - Guarda en formato XML
- **Cargar configuración**: `Settings.load("archivo.xml")` - Carga desde XML

---

## 2. LECTURA DE TAGS (INVENTARIO)

### 2.1 Lectura Básica
- **Iniciar lectura**: `reader.start()` - Comienza el inventario de tags
- **Detener lectura**: `reader.stop()` - Detiene el inventario
- **Modos de reporte**:
  - `ReportMode.Individual` - Reporta cada tag individualmente
  - `ReportMode.BatchAfterStop` - Reporta todos los tags al detener
  - `ReportMode.WaitForQuery` - Espera consulta antes de reportar

### 2.2 Información Incluida en Reportes
- **EPC**: Código único del tag
- **PC Bits**: Protocol Control bits
- **Antena**: Puerto de antena que detectó el tag
- **Tiempo**: Primera vez visto, última vez visto, conteo de veces visto
- **RSSI**: Potencia de señal recibida
- **Fase**: Fase de la señal
- **Datos de memoria**: EPC, TID, User Memory, Reserved Memory

### 2.3 Filtrado de Tags
- **Filtro por EPC**: Filtrar tags por patrón en memoria EPC
- **Filtro por User Memory**: Filtrar por datos en memoria de usuario
- **Múltiples filtros**: Combinar hasta 2 filtros (Filter1 AND Filter2, OR, etc.)
- **Operaciones de filtro**: Match, Not Match

### 2.4 Lectura Optimizada
- **Optimized Read Operations**: Leer memoria adicional (TID, User Memory) durante el inventario
- **Lectura automática**: El lector lee automáticamente datos adicionales mientras hace inventario

---

## 3. OPERACIONES CON TAGS (READ/WRITE/LOCK/KILL)

### 3.1 Operaciones de Lectura
- **Leer EPC**: Leer código EPC del tag
- **Leer User Memory**: Leer memoria de usuario (hasta 32 palabras por operación)
- **Leer TID Memory**: Leer Tag Identifier
- **Leer Reserved Memory**: Leer memoria reservada (passwords, etc.)
- **Lectura masiva**: Dividir lecturas grandes en múltiples operaciones

### 3.2 Operaciones de Escritura
- **Escribir EPC**: Cambiar el código EPC del tag
- **Escribir User Memory**: Escribir datos en memoria de usuario
- **Escribir Passwords**: Configurar access password y kill password
- **Ajustar PC Bits**: Ajustar automáticamente PC bits al cambiar longitud de EPC
- **Escritura masiva**: Escribir grandes bloques de datos

### 3.3 Operaciones de Bloqueo (Lock)
- **Bloquear EPC**: Bloquear memoria EPC (permanente o temporal)
- **Bloquear User Memory**: Bloquear memoria de usuario
- **Bloquear TID**: Bloquear Tag Identifier
- **Bloquear Passwords**: Bloquear access password y kill password
- **Estados de bloqueo**: Lock, Unlock, Permalock

### 3.4 Operaciones de Eliminación (Kill)
- **Kill Tag**: Desactivar permanentemente un tag (irreversible)
- **Requiere Kill Password**: Necesita password de kill configurado

### 3.5 Operaciones Especiales
- **Block Permalock**: Bloquear permanentemente bloques específicos de memoria
- **Margin Read**: Leer margen de operación del tag
- **Fast ID**: Lectura rápida solo del EPC

---

## 4. SECUENCIAS DE OPERACIONES

### 4.1 TagOpSequence
- **Múltiples operaciones**: Ejecutar varias operaciones en secuencia
- **Target Tag**: Aplicar operaciones solo a tags específicos (por EPC)
- **Execution Count**: Número de veces que se ejecuta la secuencia
- **Estados**: Active, Disabled
- **Triggers de parada**: Por conteo de ejecución, timeout, etc.

### 4.2 Gestión de Secuencias
- **Agregar secuencia**: `reader.addOpSequence(sequence)`
- **Eliminar secuencia**: `reader.deleteOpSequence(id)`
- **Eliminar todas**: `reader.deleteAllOpSequences()`
- **Múltiples secuencias**: El lector puede ejecutar múltiples secuencias simultáneamente

---

## 5. CONFIGURACIÓN DE ANTENAS

### 5.1 Configuración Individual
- **Habilitar/Deshabilitar**: Control por antena
- **Potencia de transmisión (Tx Power)**: Configurar en dBm o usar máximo
- **Sensibilidad de recepción (Rx Sensitivity)**: Configurar en dBm o usar máximo
- **Frecuencias**: Configurar frecuencias específicas por antena

### 5.2 Configuración de Múltiples Antenas
- **Habilitar múltiples**: Activar varias antenas simultáneamente
- **Configuración por grupo**: Configurar grupos de antenas
- **Detección de antenas**: Detectar cuando se conectan/desconectan antenas

### 5.3 Antenna Hubs
- **Soporte para hubs**: Lectores pueden usar hubs para más puertos de antena
- **Estado de hubs**: Consultar estado y fallos de hubs

---

## 6. MODOS DE OPERACIÓN DEL LECTOR

### 6.1 Reader Modes
- **AutoSetDenseReader**: Auto-optimiza para ambientes densos (recomendado)
- **DenseReaderM4**: Modo denso para región M4
- **DenseReaderM8**: Modo denso para región M8
- **MaxThroughput**: Máximo rendimiento
- **Hybrid**: Modo híbrido
- **Autoset**: Auto-configuración

### 6.2 Search Modes
- **SingleTarget**: Buscar un tag a la vez
- **DualTarget**: Buscar dos tags simultáneamente
- **DualTargetWithSuppression**: Dual con supresión

### 6.3 Session
- **Sessions 0-3**: Diferentes sesiones para control de inventario
- **Session 1**: Recomendado para ver tags una vez cada pocos segundos

---

## 7. TRIGGERS Y AUTOMATIZACIÓN

### 7.1 Auto Start (Inicio Automático)
- **Modo Periódico**: Iniciar cada X milisegundos
- **GPI Trigger**: Iniciar cuando GPI cambia de estado
- **Manual**: Inicio manual

### 7.2 Auto Stop (Parada Automática)
- **Por Duración**: Detener después de X milisegundos
- **GPI Trigger**: Detener cuando GPI cambia
- **Timeout**: Detener después de timeout

### 7.3 Triggers por GPI
- **GPI Level**: Iniciar/detener en nivel alto o bajo
- **Debounce**: Configurar tiempo de debounce para evitar rebotes
- **Múltiples GPIs**: Configurar diferentes GPIs para diferentes triggers

---

## 8. GPI/GPO (ENTRADAS/SALIDAS DE PROPÓSITO GENERAL)

### 8.1 GPI (General Purpose Input)
- **Configuración**: Habilitar/deshabilitar por puerto
- **Eventos**: Recibir notificaciones cuando GPI cambia
- **Debounce**: Configurar tiempo de estabilización
- **Uso como triggers**: Para auto start/stop

### 8.2 GPO (General Purpose Output)
- **Control directo**: `reader.setGpo(port, value)` - Activar/desactivar
- **Modos de operación**: Diferentes modos de funcionamiento
- **Configuración**: Configurar comportamiento de GPOs

---

## 9. FILTROS Y BÚSQUEDA

### 9.1 Tag Filters
- **Filter 1 y Filter 2**: Dos filtros independientes
- **Modos de combinación**: 
  - OnlyFilter1
  - OnlyFilter2
  - Filter1AndFilter2
  - Filter1OrFilter2
- **Configuración por bits**: Especificar posición, longitud, banco de memoria

### 9.2 Software Filtering
- **Filtrado en aplicación**: Filtrar tags en el código de la aplicación
- **Múltiples criterios**: Combinar múltiples condiciones

---

## 10. EVENTOS Y LISTENERS

### 10.1 Tag Report Listener
- **onTagReported()**: Se llama cuando se detectan tags
- **Información completa**: EPC, antena, tiempo, RSSI, etc.

### 10.2 Tag Operation Complete Listener
- **onTagOpComplete()**: Se llama cuando operaciones (read/write/lock/kill) completan
- **Resultados**: Estado de éxito/fallo, datos leídos, etc.

### 10.3 Otros Listeners
- **GpiChangeListener**: Cambios en GPI
- **ReaderStartListener**: Cuando el lector inicia
- **ReaderStopListener**: Cuando el lector detiene
- **AntennaChangeListener**: Cambios en estado de antenas
- **ConnectionLostListener**: Pérdida de conexión
- **BufferOverflowListener**: Desbordamiento de buffer
- **BufferWarningListener**: Advertencias de buffer

---

## 11. CARACTERÍSTICAS ESPECIALES

### 11.1 QT (Impinj QT)
- **QT Get**: Leer configuración QT del tag
- **QT Set**: Configurar modo QT del tag
- **Data Profiles**: Public, Private
- **Access Range**: Normal, Reduced
- **Persistence**: Permanent, Session

### 11.2 Low Duty Cycle
- **Modo automático**: Reduce ciclo de trabajo cuando no hay tags
- **Ahorro de energía**: Optimiza consumo cuando está inactivo

### 11.3 Keepalives
- **Monitoreo de conexión**: Verificar que la conexión está activa
- **Timeout**: Detectar pérdida de conexión

### 11.4 Disconnected Operation
- **Operación sin conexión**: Continuar operando si se pierde conexión temporalmente

---

## 12. CONFIGURACIÓN DE FRECUENCIAS Y POTENCIA

### 12.1 Frecuencias de Transmisión
- **Tabla de frecuencias**: Configurar frecuencias específicas
- **Reduced Power Frequencies**: Reducir potencia en frecuencias específicas
- **Regulaciones**: Soporte para diferentes regiones regulatorias

### 12.2 Potencia de Transmisión
- **Tx Power Ramp**: Variar potencia gradualmente
- **Configuración por antena**: Diferente potencia por antena
- **Máximo permitido**: Usar máxima potencia o configurar valor específico

### 12.3 Sensibilidad de Recepción
- **Rx Sensitivity Ramp**: Variar sensibilidad
- **Tabla de sensibilidad**: Configurar niveles específicos
- **Máxima sensibilidad**: Usar máxima o configurar valor

---

## 13. CARACTERÍSTICAS ESPACIALES (xArray - No aplica a R220)

### 13.1 Location (Ubicación)
- **Reportes de ubicación**: Coordenadas X, Y, Z de tags
- **Placement Config**: Configurar posición del xArray
- **Confidence Factors**: Factores de confianza de ubicación

### 13.2 Direction (Dirección)
- **Dirección de movimiento**: Detectar dirección de tags
- **Field of View**: Campo de visión estrecho o amplio
- **Tracking**: Seguimiento de tags en movimiento

---

## 14. SEGURIDAD Y CONEXIÓN

### 14.1 TLS/SSL
- **Conexión segura**: `ReadTagsOverTLS` - Conexión encriptada
- **Certificados**: Soporte para certificados SSL

### 14.2 RShell
- **Acceso remoto**: Ejecutar comandos remotos en el lector
- **SSH/Telnet**: Diferentes protocolos de conexión
- **Comandos**: Enviar comandos shell al lector

---

## 15. DIAGNÓSTICOS Y MONITOREO

### 15.1 Estado del Lector
- **Temperatura**: Monitorear temperatura
- **Estado de conexión**: Verificar si está conectado
- **Estado de singulación**: Si está leyendo tags
- **Tilt Sensor**: Sensor de inclinación (en algunos modelos)

### 15.2 Diagnostic Reports
- **Reportes de diagnóstico**: Información detallada del sistema
- **Estado de antenas**: Estado individual de cada antena
- **Estado de hubs**: Estado de antenna hubs

---

## 16. OPTIMIZACIONES Y RENDIMIENTO

### 16.1 Optimizaciones de Lectura
- **Optimized Read**: Leer datos adicionales durante inventario
- **Fast ID**: Lectura rápida solo de EPC
- **Bulk Operations**: Operaciones masivas optimizadas

### 16.2 Configuración de Rendimiento
- **Reader Modes**: Modos optimizados para diferentes escenarios
- **Search Modes**: Modos de búsqueda optimizados
- **Session Management**: Gestión de sesiones para mejor rendimiento

---

## 17. EJEMPLOS DE USO COMUNES

### 17.1 Lectura Simple de Tags
```java
ImpinjReader reader = new ImpinjReader();
reader.connect(hostname);
Settings settings = reader.queryDefaultSettings();
reader.applySettings(settings);
reader.setTagReportListener(new TagReportListener());
reader.start();
```

### 17.2 Escritura de EPC
```java
TagOpSequence seq = new TagOpSequence();
TagWriteOp writeOp = new TagWriteOp();
writeOp.setMemoryBank(MemoryBank.Epc);
writeOp.setData(TagData.fromHexString("nuevoEPC"));
seq.getOps().add(writeOp);
reader.addOpSequence(seq);
```

### 17.3 Lectura con Filtro
```java
TagFilter filter = settings.getFilters().getTagFilter1();
filter.setBitCount(16);
filter.setMemoryBank(MemoryBank.Epc);
filter.setTagMask("E200");
filter.setFilterOp(TagFilterOp.Match);
settings.getFilters().setMode(TagFilterMode.OnlyFilter1);
```

### 17.4 Múltiples Lectores
```java
ArrayList<ImpinjReader> readers = new ArrayList<>();
for (String hostname : hostnames) {
    ImpinjReader reader = new ImpinjReader();
    reader.connect(hostname);
    readers.add(reader);
}
```

---

## 18. CONSIDERACIONES PARA IMPINJ R220

### 18.1 Características del R220
- **4 puertos de antena**: Soporta hasta 4 antenas
- **GPI/GPO**: Múltiples puertos de entrada/salida
- **Reader Modes**: Soporta todos los modos estándar
- **Frecuencias**: Configurable según región

### 18.2 Limitaciones
- **No soporta xArray features**: Location y Direction no aplican
- **No tilt sensor**: El R220 no tiene sensor de inclinación
- **Antenna Hubs**: Puede usar hubs para expandir puertos

---

## 19. MEJORES PRÁCTICAS

### 19.1 Gestión de Conexión
- Siempre usar try-catch para manejar excepciones
- Verificar estado antes de operaciones críticas
- Implementar reconexión automática si es necesario

### 19.2 Gestión de Tags
- Usar filtros cuando sea posible para reducir carga
- Configurar session apropiada según necesidad
- Usar AutoSetDenseReader para ambientes con muchos tags

### 19.3 Operaciones con Tags
- Siempre especificar TargetTag para operaciones destructivas (Kill, Lock)
- Verificar resultados de operaciones antes de continuar
- Usar secuencias para operaciones múltiples relacionadas

---

## 20. RECURSOS ADICIONALES

### 20.1 Documentación
- Javadoc completo incluido en el SDK
- Ejemplos de código en la carpeta `samples/`
- Release Notes con información de versión

### 20.2 Clases Principales
- **ImpinjReader**: Clase principal para controlar el lector
- **Settings**: Configuración completa del lector
- **Tag**: Representa un tag RFID
- **TagOpSequence**: Secuencia de operaciones
- **TagReport**: Reporte de tags detectados

---

## CONCLUSIÓN

El Octane SDK proporciona acceso completo a todas las funcionalidades de los lectores Impinj R220. Con este SDK puedes:

✅ Leer tags con múltiples configuraciones
✅ Escribir y modificar datos en tags
✅ Bloquear y proteger memoria
✅ Gestionar múltiples lectores simultáneamente
✅ Configurar antenas, frecuencias y potencia
✅ Automatizar operaciones con triggers
✅ Filtrar y buscar tags específicos
✅ Monitorear estado y eventos del lector
✅ Optimizar rendimiento según necesidades

El SDK es robusto, bien documentado y proporciona ejemplos para la mayoría de casos de uso comunes.

