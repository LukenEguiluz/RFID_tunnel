# 🧪 Webapp de Prueba - RFID Gateway

Webapp simple para probar el sistema de sesiones RFID. Permite iniciar lectura, ver tags únicos en tiempo real y detener lectura.

## 🚀 Inicio Rápido

### 1. Construir e Iniciar

```bash
cd webapp-test
docker-compose up -d --build
```

### 2. Acceder a la Webapp

Abre en tu navegador:
```
http://localhost:3000
```

### 3. Configurar

1. **URL del Gateway**: Por defecto `http://192.168.0.189:8080`
   - Cambia si tu gateway está en otra IP/puerto
   
2. **ID del Grupo** o **ID del Lector**:
   - Ingresa el `groupId` (ej: `entrada-principal`)
   - O ingresa el `readerId` (ej: `reader-1`)

### 4. Usar

1. Haz clic en **"▶️ Iniciar Lectura"**
2. Observa los **tags únicos** aparecer en tiempo real
3. Haz clic en **"⏸️ Detener Lectura"** cuando termines
4. Verás la lista final de tags únicos detectados

## 📋 Características

- ✅ Inicia lectura con grupo o lector individual
- ✅ Muestra solo tags únicos (sin duplicados)
- ✅ Actualización en tiempo real (polling cada 1 segundo)
- ✅ Contador de tags únicos
- ✅ Log de eventos
- ✅ Interfaz simple y clara

## 🔧 Detener

```bash
docker-compose down
```

## 📝 Notas

- La webapp hace polling cada 1 segundo para obtener tags nuevos
- Solo muestra tags únicos (sin duplicados)
- Los tags se ordenan alfabéticamente
- El log muestra todos los eventos importantes

