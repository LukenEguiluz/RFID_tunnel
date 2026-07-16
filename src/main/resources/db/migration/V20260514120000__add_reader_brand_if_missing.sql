-- Lectores existentes sin columna brand rompen Hibernate (SQLGrammarException al leer).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'readers'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'readers' AND column_name = 'brand'
    ) THEN
        ALTER TABLE readers ADD COLUMN brand VARCHAR(32);
        UPDATE readers SET brand = 'IMPINJ_OCTANE' WHERE brand IS NULL;
        ALTER TABLE readers ALTER COLUMN brand SET DEFAULT 'IMPINJ_OCTANE';
        ALTER TABLE readers ALTER COLUMN brand SET NOT NULL;
    END IF;
END $$;
