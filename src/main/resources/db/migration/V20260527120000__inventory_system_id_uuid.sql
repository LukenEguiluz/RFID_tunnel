-- Migrar inventory_systems.id y referencias de VARCHAR a UUID

-- 1) Sistemas: columna UUID temporal
ALTER TABLE inventory_systems ADD COLUMN IF NOT EXISTS id_uuid UUID;

UPDATE inventory_systems
SET id_uuid = CASE
    WHEN id ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$'
        THEN id::uuid
    ELSE gen_random_uuid()
END
WHERE id_uuid IS NULL;

-- 2) inventory_system_epc_states
ALTER TABLE inventory_system_epc_states ADD COLUMN IF NOT EXISTS system_id_uuid UUID;

UPDATE inventory_system_epc_states t
SET system_id_uuid = s.id_uuid
FROM inventory_systems s
WHERE t.system_id = s.id AND t.system_id_uuid IS NULL;

ALTER TABLE inventory_system_epc_states DROP COLUMN IF EXISTS system_id;
ALTER TABLE inventory_system_epc_states RENAME COLUMN system_id_uuid TO system_id;
ALTER TABLE inventory_system_epc_states ALTER COLUMN system_id SET NOT NULL;

-- 3) epc_presence_events
ALTER TABLE epc_presence_events ADD COLUMN IF NOT EXISTS system_id_uuid UUID;

UPDATE epc_presence_events t
SET system_id_uuid = s.id_uuid
FROM inventory_systems s
WHERE t.system_id = s.id AND t.system_id_uuid IS NULL;

ALTER TABLE epc_presence_events DROP COLUMN IF EXISTS system_id;
ALTER TABLE epc_presence_events RENAME COLUMN system_id_uuid TO system_id;
ALTER TABLE epc_presence_events ALTER COLUMN system_id SET NOT NULL;

-- 4) webhook_failed_events (si existe)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'webhook_failed_events') THEN
        ALTER TABLE webhook_failed_events ADD COLUMN IF NOT EXISTS system_id_uuid UUID;
        UPDATE webhook_failed_events t
        SET system_id_uuid = s.id_uuid
        FROM inventory_systems s
        WHERE t.system_id = s.id AND t.system_id_uuid IS NULL;
        ALTER TABLE webhook_failed_events DROP COLUMN IF EXISTS system_id;
        ALTER TABLE webhook_failed_events RENAME COLUMN system_id_uuid TO system_id;
        ALTER TABLE webhook_failed_events ALTER COLUMN system_id SET NOT NULL;
    END IF;
END $$;

-- 5) inventory_system_readers
ALTER TABLE inventory_system_readers ADD COLUMN IF NOT EXISTS system_id_uuid UUID;

UPDATE inventory_system_readers t
SET system_id_uuid = s.id_uuid
FROM inventory_systems s
WHERE t.system_id = s.id AND t.system_id_uuid IS NULL;

ALTER TABLE inventory_system_readers DROP CONSTRAINT IF EXISTS inventory_system_readers_system_id_fkey;
ALTER TABLE inventory_system_readers DROP COLUMN IF EXISTS system_id;
ALTER TABLE inventory_system_readers RENAME COLUMN system_id_uuid TO system_id;
ALTER TABLE inventory_system_readers ALTER COLUMN system_id SET NOT NULL;

-- 6) readers.inventory_system_id (nullable)
ALTER TABLE readers ADD COLUMN IF NOT EXISTS inventory_system_id_uuid UUID;

UPDATE readers r
SET inventory_system_id_uuid = s.id_uuid
FROM inventory_systems s
WHERE r.inventory_system_id = s.id AND r.inventory_system_id_uuid IS NULL;

ALTER TABLE readers DROP COLUMN IF EXISTS inventory_system_id;
ALTER TABLE readers RENAME COLUMN inventory_system_id_uuid TO inventory_system_id;

-- 7) PK inventory_systems
ALTER TABLE inventory_systems DROP CONSTRAINT IF EXISTS inventory_systems_pkey;
ALTER TABLE inventory_systems DROP COLUMN IF EXISTS id;
ALTER TABLE inventory_systems RENAME COLUMN id_uuid TO id;
ALTER TABLE inventory_systems ALTER COLUMN id SET NOT NULL;
ALTER TABLE inventory_systems ADD PRIMARY KEY (id);

-- 8) FK inventory_system_readers -> inventory_systems
ALTER TABLE inventory_system_readers
    ADD CONSTRAINT inventory_system_readers_system_id_fkey
    FOREIGN KEY (system_id) REFERENCES inventory_systems (id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_inv_epc_states_system ON inventory_system_epc_states (system_id);
CREATE INDEX IF NOT EXISTS idx_epc_presence_system_time ON epc_presence_events (system_id, occurred_at);
