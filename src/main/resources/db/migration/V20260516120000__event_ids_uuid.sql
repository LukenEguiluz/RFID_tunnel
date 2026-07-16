-- Identificadores de eventos como UUID (único global). Solo aplica si la columna id sigue siendo entera.

DO $$
DECLARE
    dt text;
BEGIN
    SELECT c.data_type INTO dt
    FROM information_schema.columns c
    WHERE c.table_schema = 'public' AND c.table_name = 'tag_events' AND c.column_name = 'id';

    IF dt IS NOT NULL AND dt IN ('bigint', 'integer', 'smallint') THEN
        ALTER TABLE tag_events ADD COLUMN id_uuid uuid;
        UPDATE tag_events SET id_uuid = gen_random_uuid();
        ALTER TABLE tag_events ALTER COLUMN id_uuid SET NOT NULL;
        ALTER TABLE tag_events DROP CONSTRAINT IF EXISTS tag_events_pkey;
        ALTER TABLE tag_events DROP COLUMN id;
        ALTER TABLE tag_events RENAME COLUMN id_uuid TO id;
        ALTER TABLE tag_events ADD PRIMARY KEY (id);
        DROP SEQUENCE IF EXISTS tag_events_id_seq CASCADE;
    END IF;
END $$;

DO $$
DECLARE
    dt text;
BEGIN
    SELECT c.data_type INTO dt
    FROM information_schema.columns c
    WHERE c.table_schema = 'public' AND c.table_name = 'epc_presence_events' AND c.column_name = 'id';

    IF dt IS NOT NULL AND dt IN ('bigint', 'integer', 'smallint') THEN
        ALTER TABLE epc_presence_events ADD COLUMN id_uuid uuid;
        UPDATE epc_presence_events SET id_uuid = gen_random_uuid();
        ALTER TABLE epc_presence_events ALTER COLUMN id_uuid SET NOT NULL;
        ALTER TABLE epc_presence_events DROP CONSTRAINT IF EXISTS epc_presence_events_pkey;
        ALTER TABLE epc_presence_events DROP COLUMN id;
        ALTER TABLE epc_presence_events RENAME COLUMN id_uuid TO id;
        ALTER TABLE epc_presence_events ADD PRIMARY KEY (id);
        DROP SEQUENCE IF EXISTS epc_presence_events_id_seq CASCADE;
    END IF;
END $$;
