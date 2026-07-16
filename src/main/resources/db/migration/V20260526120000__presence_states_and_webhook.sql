-- Estados de presencia (PRESENT / LOST / REMOVED) y configuración webhook por sistema

ALTER TABLE inventory_systems
    ADD COLUMN IF NOT EXISTS cycles_to_lost INT NOT NULL DEFAULT 3,
    ADD COLUMN IF NOT EXISTS cycles_to_remove_after_lost INT NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS webhook_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS webhook_url TEXT,
    ADD COLUMN IF NOT EXISTS webhook_secret TEXT;

ALTER TABLE inventory_system_epc_states
    ADD COLUMN IF NOT EXISTS presence_state VARCHAR(10),
    ADD COLUMN IF NOT EXISTS missed_cycles INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS lost_cycles INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_rssi DOUBLE PRECISION;

UPDATE inventory_system_epc_states
SET presence_state = 'PRESENT'
WHERE presence_state IS NULL AND present = true;

UPDATE inventory_system_epc_states
SET presence_state = 'REMOVED'
WHERE presence_state IS NULL AND present = false;

UPDATE inventory_system_epc_states
SET presence_state = 'PRESENT'
WHERE presence_state IS NULL;

ALTER TABLE inventory_system_epc_states
    ALTER COLUMN presence_state SET NOT NULL;

ALTER TABLE epc_presence_events
    ADD COLUMN IF NOT EXISTS previous_state VARCHAR(10),
    ADD COLUMN IF NOT EXISTS new_state VARCHAR(10),
    ADD COLUMN IF NOT EXISTS rssi DOUBLE PRECISION;

UPDATE epc_presence_events SET event_type = 'ADDED' WHERE event_type = 'ADD';
UPDATE epc_presence_events SET event_type = 'REMOVED' WHERE event_type = 'REMOVE';

ALTER TABLE epc_presence_events
    ALTER COLUMN event_type TYPE VARCHAR(20);

CREATE TABLE IF NOT EXISTS webhook_failed_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    system_id       VARCHAR(64) NOT NULL,
    event_id        UUID NOT NULL,
    event_type      VARCHAR(32) NOT NULL,
    payload_json    TEXT NOT NULL,
    attempts        INT NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_attempt_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_webhook_failed_system ON webhook_failed_events (system_id, created_at DESC);
