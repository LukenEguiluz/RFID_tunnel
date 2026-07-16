-- Quitar restricciones UNIQUE en inventory_systems (p. ej. sobre name) salvo la PK.
-- Hibernate puede haber generado nombres distintos a inventory_systems_name_key.
DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN
    SELECT c.conname
    FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    WHERE t.relname = 'inventory_systems' AND c.contype = 'u'
  LOOP
    EXECUTE format('ALTER TABLE inventory_systems DROP CONSTRAINT IF EXISTS %I', r.conname);
  END LOOP;
END$$;
