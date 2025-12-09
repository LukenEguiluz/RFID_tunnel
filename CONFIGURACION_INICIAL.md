# Guía de Configuración Inicial

## 🚀 Pasos para Configurar el Gateway

### 1. Preparar Base de Datos

#### Opción A: Con Docker Compose (Recomendado)
```bash
# Iniciar solo PostgreSQL primero
docker-compose up -d postgres

# Esperar a que PostgreSQL esté listo (unos segundos)
```

#### Opción B: PostgreSQL Manual
```sql
CREATE DATABASE rfidgateway;
CREATE USER rfiduser WITH PASSWORD 'changeme';
GRANT ALL PRIVILEGES ON DATABASE rfidgateway TO rfiduser;
```

### 2. Iniciar el Gateway

```bash
# Compilar
mvn clean package

# Con Docker Compose
docker-compose up -d

# O manualmente
java -jar target/rfid-gateway-1.0.0.jar
```

El gateway creará automáticamente las tablas en la base de datos.

### 3. Configurar Lectores

Tienes dos opciones para configurar lectores:

#### Opción A: Insertar Directamente en PostgreSQL

```sql
-- Conectar a la base de datos
\c rfidgateway

-- Insertar lector 1
INSERT INTO readers (id, name, hostname, enabled) 
VALUES ('reader-1', 'Lector Entrada Principal', '192.168.1.100', true);

-- Insertar antenas del lector 1
INSERT INTO antennas (id, reader_id, name, port_number, enabled) 
VALUES 
  ('reader-1-antenna-1', 'reader-1', 'Antena Principal', 1, true),
  ('reader-1-antenna-2', 'reader-1', 'Antena Secundaria', 2, true);

-- Insertar lector 2
INSERT INTO readers (id, name, hostname, enabled) 
VALUES ('reader-2', 'Lector Almacén', '192.168.1.101', true);

-- Insertar antenas del lector 2
INSERT INTO antennas (id, reader_id, name, port_number, enabled) 
VALUES 
  ('reader-2-antenna-1', 'reader-2', 'Antena Principal', 1, true),
  ('reader-2-antenna-2', 'reader-2', 'Antena Secundaria', 2, true);
```

**⚠️ IMPORTANTE**: Reemplaza las IPs (`192.168.1.100`, `192.168.1.101`) con las IPs reales de tus lectores R220.

#### Opción B: Usar API REST (cuando el gateway esté corriendo)

```bash
# Agregar lector 1
curl -X POST http://localhost:8080/api/readers \
  -H "Content-Type: application/json" \
  -d '{
    "id": "reader-1",
    "name": "Lector Entrada Principal",
    "hostname": "192.168.1.100",
    "enabled": true
  }'

# Agregar antenas (requiere endpoint adicional o insertar directamente en BD)
```

### 4. Verificar Conexión

```bash
# Ver estado de lectores
curl http://localhost:8080/api/readers

# Ver estado general
curl http://localhost:8080/api/status
```

### 5. Verificar que los Lectores se Conecten

El gateway intentará conectar automáticamente a todos los lectores habilitados al iniciar.

Revisa los logs:
```bash
# Con Docker
docker-compose logs -f gateway

# Deberías ver mensajes como:
# "Conectando a lector Lector Entrada Principal (reader-1) en 192.168.1.100"
# "Lector Lector Entrada Principal conectado y leyendo exitosamente"
```

### 6. Probar Detección de Tags

Una vez conectados, los lectores comenzarán a detectar tags automáticamente.

```bash
# Ver últimos eventos
curl http://localhost:8080/api/events/latest?limit=10
```

## 🔧 Configuración de Antenas

### Configurar Potencia y Sensibilidad

```sql
-- Configurar potencia de transmisión (en dBm)
UPDATE antennas 
SET tx_power_dbm = 20.0 
WHERE id = 'reader-1-antenna-1';

-- Configurar sensibilidad de recepción (en dBm)
UPDATE antennas 
SET rx_sensitivity_dbm = -70.0 
WHERE id = 'reader-1-antenna-1';
```

**Nota**: Después de cambiar la configuración de antenas, reinicia el lector:
```bash
curl -X POST http://localhost:8080/api/readers/reader-1/stop
curl -X POST http://localhost:8080/api/readers/reader-1/start
```

## 📊 Verificar Funcionamiento

### 1. Ver Lectores Conectados

```bash
curl http://localhost:8080/api/status
```

Deberías ver:
```json
{
  "totalReaders": 2,
  "connectedReaders": 2,
  "readingReaders": 2,
  "readers": [...]
}
```

### 2. Ver Eventos en Tiempo Real

Conecta un cliente WebSocket a `ws://localhost:8080/ws/events` o usa la API:

```bash
# Ver últimos 20 eventos
curl http://localhost:8080/api/events/latest?limit=20
```

### 3. Filtrar Eventos

```bash
# Eventos de un EPC específico
curl "http://localhost:8080/api/events?epc=E200123456789012"

# Eventos de un lector
curl "http://localhost:8080/api/events?reader=reader-1"

# Eventos de una antena
curl "http://localhost:8080/api/events?antenna=reader-1-antenna-1"
```

## 🐛 Solución de Problemas Comunes

### El lector no se conecta

1. **Verificar IP del lector**:
   ```bash
   ping 192.168.1.100  # Reemplaza con la IP de tu lector
   ```

2. **Verificar que el lector esté encendido y en la red**

3. **Verificar configuración en BD**:
   ```sql
   SELECT * FROM readers WHERE id = 'reader-1';
   ```

4. **Revisar logs del gateway**:
   
   **PowerShell:**
   ```powershell
   docker-compose logs gateway | Select-String "reader-1"
   ```
   
   **Linux/Mac:**
   ```bash
   docker-compose logs gateway | grep "reader-1"
   ```

### No se detectan tags

1. **Verificar que el lector esté leyendo**:
   ```bash
   curl http://localhost:8080/api/readers/reader-1/status
   ```
   Debe mostrar `"reading": true`

2. **Verificar que las antenas estén habilitadas**:
   ```sql
   SELECT * FROM antennas WHERE reader_id = 'reader-1';
   ```

3. **Verificar que haya tags cerca de las antenas**

### Base de datos no conecta

1. **Verificar que PostgreSQL esté corriendo**:
   ```bash
   docker-compose ps postgres
   ```

2. **Verificar credenciales en `application.yml`**

3. **Verificar que la base de datos exista**:
   ```sql
   \l  # Listar bases de datos
   ```

## 📝 Notas Importantes

1. **IPs de Lectores**: Asegúrate de que las IPs en la configuración sean correctas y accesibles desde el servidor del gateway.

2. **Nombres de Lectores**: Puedes usar cualquier nombre descriptivo. Ejemplos:
   - "Lector Entrada Principal"
   - "Lector Almacén"
   - "Lector Salida"

3. **Puertos de Antenas**: Los R220 tienen 4 puertos, pero según tu configuración solo usas 2. Asegúrate de usar los puertos correctos (1 y 2).

4. **Reconexión Automática**: Si un lector se desconecta, el gateway intentará reconectarlo automáticamente cada 30 segundos.

5. **Persistencia de Eventos**: Todos los eventos se guardan en PostgreSQL. Para mantener el rendimiento, considera limpiar eventos antiguos periódicamente.

## ✅ Checklist de Configuración

- [ ] PostgreSQL configurado y corriendo
- [ ] Gateway compilado y corriendo
- [ ] Lectores insertados en la base de datos
- [ ] Antenas configuradas para cada lector
- [ ] IPs de lectores correctas
- [ ] Lectores conectados (verificar en `/api/status`)
- [ ] Eventos siendo detectados (verificar en `/api/events/latest`)
- [ ] WebSocket funcionando (opcional, para tiempo real)

---

**¡Listo! Tu gateway debería estar funcionando correctamente.**


