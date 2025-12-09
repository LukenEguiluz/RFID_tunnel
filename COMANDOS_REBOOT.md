# 🔄 Comandos de Reinicio y Reboot

## Endpoints REST Disponibles

### 1. Reset de Conexión
Reinicia la conexión del lector (desconecta y reconecta después de 2 segundos).

```bash
POST /api/readers/{id}/reset
```

**Ejemplo:**
```bash
curl -X POST http://localhost:8080/api/readers/lector-1/reset
```

**Respuesta:**
```json
{
  "message": "Reader reset and reconnecting",
  "readerId": "lector-1"
}
```

---

### 2. Reboot Completo
Ejecuta un reboot completo del lector (desconecta y reconecta después de 5 segundos).

```bash
POST /api/readers/{id}/reboot
```

**Ejemplo:**
```bash
curl -X POST http://localhost:8080/api/readers/lector-1/reboot
```

**Respuesta:**
```json
{
  "message": "Reader reboot initiated, will reconnect in 5 seconds",
  "readerId": "lector-1"
}
```

---

### 3. Reset de Antenas
Reinicia solo la configuración de antenas sin desconectar el lector.

```bash
POST /api/readers/{id}/antennas/reset
```

**Ejemplo:**
```bash
curl -X POST http://localhost:8080/api/readers/lector-1/antennas/reset
```

**Respuesta:**
```json
{
  "message": "Antennas configuration reset",
  "readerId": "lector-1"
}
```

---

## Cuándo Usar Cada Comando

### 🔄 Reset (`/reset`)
- **Cuándo usar:** Cuando el lector está conectado pero no está leyendo tags
- **Qué hace:** Desconecta completamente, espera 2 segundos y reconecta
- **Tiempo:** ~2-3 segundos

### 🔁 Reboot (`/reboot`)
- **Cuándo usar:** Cuando hay problemas persistentes o necesitas un reinicio completo
- **Qué hace:** Desconecta completamente, espera 5 segundos y reconecta
- **Tiempo:** ~5-6 segundos

### 📡 Reset de Antenas (`/antennas/reset`)
- **Cuándo usar:** Cuando solo necesitas reiniciar la configuración de antenas sin desconectar
- **Qué hace:** Detiene lectura, reconfigura antenas y reinicia lectura
- **Tiempo:** ~1 segundo

---

## Ejemplos con PowerShell

```powershell
# Reset de conexión
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/readers/lector-1/reset"

# Reboot completo
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/readers/lector-1/reboot"

# Reset de antenas
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/readers/lector-1/antennas/reset"
```

---

## Notas Importantes

1. **Configuración por Defecto:** Si no hay antenas configuradas en la base de datos, el gateway ahora usa solo la **antena 1** (igual que el ejemplo que funciona).

2. **Configuración Igual al Ejemplo:** El gateway ahora usa la misma configuración que `TestLecturaSimple.java`:
   - Modo: `AutoSetDenseReader`
   - Búsqueda: `SingleTarget`
   - Sesión: `1`
   - Antena 1 con potencia y sensibilidad máximas

3. **Logs:** Todos los comandos generan logs detallados que puedes ver en:
   ```bash
   docker-compose logs -f gateway
   ```

4. **Estado del Lector:** Puedes verificar el estado después de cualquier comando:
   ```bash
   GET /api/readers/{id}/status
   ```

