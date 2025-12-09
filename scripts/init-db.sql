-- Script de inicialización de base de datos
-- Ejecutar después de crear la base de datos

-- Crear tablas (se crearán automáticamente con JPA, pero aquí está el esquema)

-- Tabla de lectores
CREATE TABLE IF NOT EXISTS readers (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    hostname VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    is_connected BOOLEAN DEFAULT false,
    is_reading BOOLEAN DEFAULT false,
    last_seen TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de antenas
CREATE TABLE IF NOT EXISTS antennas (
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

-- Tabla de eventos de tags
CREATE TABLE IF NOT EXISTS tag_events (
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

-- Índices para mejor rendimiento
CREATE INDEX IF NOT EXISTS idx_epc ON tag_events(epc);
CREATE INDEX IF NOT EXISTS idx_reader ON tag_events(reader_id);
CREATE INDEX IF NOT EXISTS idx_antenna ON tag_events(antenna_id);
CREATE INDEX IF NOT EXISTS idx_detected_at ON tag_events(detected_at);

-- Ejemplo: Insertar lectores (modificar según tu configuración)
-- Descomentar y modificar según tus lectores:

/*
INSERT INTO readers (id, name, hostname, enabled) 
VALUES 
  ('reader-1', 'Lector Entrada Principal', '192.168.1.100', true),
  ('reader-2', 'Lector Almacén', '192.168.1.101', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO antennas (id, reader_id, name, port_number, enabled) 
VALUES 
  ('reader-1-antenna-1', 'reader-1', 'Antena Principal', 1, true),
  ('reader-1-antenna-2', 'reader-1', 'Antena Secundaria', 2, true),
  ('reader-2-antenna-1', 'reader-2', 'Antena Principal', 1, true),
  ('reader-2-antenna-2', 'reader-2', 'Antena Secundaria', 2, true)
ON CONFLICT (id) DO NOTHING;
*/



