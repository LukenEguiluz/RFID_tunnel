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
- ✅ **Base de datos local (IndexedDB)** para almacenar información de productos
- ✅ **Generación automática de datos mock** cuando se detecta un EPC nuevo
- ✅ **Edición de información de productos** (descripción, caducidad, lote)
- ✅ **Visualización de información** junto a cada EPC detectado

## 🔧 Detener

```bash
docker-compose down
```

## 📝 Notas

- La webapp hace polling cada 1 segundo para obtener tags nuevos
- Solo muestra tags únicos (sin duplicados)
- Los tags se ordenan alfabéticamente
- El log muestra todos los eventos importantes

## 💾 Base de Datos de Productos

La webapp incluye un sistema de base de datos local (IndexedDB) que:

### Funcionalidad Automática
- **Detección de EPCs nuevos**: Cuando se detecta un EPC que no está en la base de datos, se generan automáticamente datos mock:
  - Descripción del producto (aleatoria de un catálogo predefinido)
  - Fecha de caducidad (entre 6 meses y 3 años desde hoy)
  - Número de lote (formato: L{YYYY}-{NNN})

### Funcionalidad Manual
- **Editar información**: Haz clic en el botón "✏️ Editar" o "✏️ Agregar Info" en cualquier tag para:
  - Modificar la descripción del producto
  - Cambiar la fecha de caducidad
  - Actualizar el número de lote

### Visualización
- Los tags muestran la información del producto junto al EPC
- Badge "Mock" indica que los datos fueron generados automáticamente
- Badge "Editado" indica que la información fue modificada manualmente
- Los datos se almacenan localmente en el navegador (IndexedDB)




