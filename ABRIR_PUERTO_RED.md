# 🌐 Abrir Puerto en la Red Local

Esta guía explica cómo abrir el puerto 8080 del Gateway RFID para que sea accesible desde otras máquinas en tu red local.

## 📋 Pasos Rápidos

### 1. Configurar Spring Boot (Ya hecho)

El archivo `application.yml` ya está configurado para escuchar en todas las interfaces:
```yaml
server:
  port: 8080
  address: 0.0.0.0  # Escuchar en todas las interfaces
```

### 2. Abrir Puerto en Firewall de Windows

**Opción A: Desde PowerShell (Administrador)**

```powershell
# Abrir puerto 8080 TCP
New-NetFirewallRule -DisplayName "RFID Gateway" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow

# Verificar que se creó
Get-NetFirewallRule -DisplayName "RFID Gateway"
```

**Opción B: Desde Interfaz Gráfica**

1. Abre "Firewall de Windows Defender" (busca "firewall" en el menú inicio)
2. Haz clic en "Configuración avanzada"
3. En el panel izquierdo, haz clic en "Reglas de entrada"
4. En el panel derecho, haz clic en "Nueva regla..."
5. Selecciona "Puerto" → Siguiente
6. Selecciona "TCP" y escribe `8080` → Siguiente
7. Selecciona "Permitir la conexión" → Siguiente
8. Marca todas las opciones (Dominio, Privada, Pública) → Siguiente
9. Nombre: "RFID Gateway" → Finalizar

### 3. Obtener tu IP Local

Tu IP local es: **192.168.0.189** (según tu configuración actual)

Para verificar:
```powershell
ipconfig | findstr /i "IPv4"
```

### 4. Reiniciar el Gateway

Si el gateway ya está corriendo, reinícialo para que tome la nueva configuración:

```bash
# Si está en Docker
docker-compose restart gateway

# Si está ejecutándose directamente
# Detén y vuelve a iniciar la aplicación
```

### 5. Probar Acceso

**Desde otra máquina en la red:**

```bash
# Reemplaza 192.168.0.189 con tu IP
curl http://192.168.0.189:8080/api/readers
```

**Desde un navegador:**
```
http://192.168.0.189:8080
```

---

## 🔍 Verificar que Funciona

### Desde la misma máquina:
```bash
curl http://localhost:8080/api/readers
```

### Desde otra máquina en la red:
```bash
curl http://192.168.0.189:8080/api/readers
```

Si ambos funcionan, el puerto está abierto correctamente.

---

## 🌐 URLs para Webapp Externa

Una vez configurado, tu webapp puede usar estas URLs:

**API REST:**
```
http://192.168.0.189:8080/api/sessions/start
http://192.168.0.189:8080/api/sessions/{sessionId}
http://192.168.0.189:8080/api/sessions/{sessionId}/stop
```

**Interfaz Web:**
```
http://192.168.0.189:8080
http://192.168.0.189:8080/readers
```

**WebSocket (si lo usas):**
```
ws://192.168.0.189:8080/ws/events
```

---

## ⚠️ Notas de Seguridad

1. **Solo red local**: El puerto está abierto solo en tu red local, no en internet
2. **Temporal**: Si quieres cerrarlo después:
   ```powershell
   Remove-NetFirewallRule -DisplayName "RFID Gateway"
   ```
3. **Producción**: Para producción, considera usar un proxy reverso (nginx) con HTTPS

---

## 🐛 Solución de Problemas

### No puedo acceder desde otra máquina

1. **Verifica el firewall:**
   ```powershell
   Get-NetFirewallRule -DisplayName "RFID Gateway" | Format-List
   ```

2. **Verifica que el gateway esté escuchando:**
   ```powershell
   netstat -an | findstr 8080
   ```
   Deberías ver: `0.0.0.0:8080` o `:::8080`

3. **Verifica la IP:**
   ```powershell
   ipconfig
   ```
   Asegúrate de usar la IP correcta de tu red local

4. **Verifica que estén en la misma red:**
   - Ambas máquinas deben estar en la misma red WiFi/LAN
   - Verifica que no haya un firewall de router bloqueando

### El gateway no inicia

Si ves errores al iniciar, verifica que el puerto 8080 no esté en uso:
```powershell
netstat -ano | findstr :8080
```

---

## 📝 Ejemplo de Uso desde Webapp Externa

```javascript
// En tu webapp (en otra máquina)
const GATEWAY_URL = 'http://192.168.0.189:8080';

// Iniciar sesión
const response = await fetch(`${GATEWAY_URL}/api/sessions/start`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ groupId: 'entrada-principal' })
});

const session = await response.json();
console.log('Sesión iniciada:', session.sessionId);
```

---

## 🔒 Cerrar el Puerto (Cuando ya no lo necesites)

```powershell
Remove-NetFirewallRule -DisplayName "RFID Gateway"
```

O desde la interfaz gráfica:
1. Firewall de Windows → Configuración avanzada
2. Reglas de entrada
3. Busca "RFID Gateway"
4. Click derecho → Eliminar

