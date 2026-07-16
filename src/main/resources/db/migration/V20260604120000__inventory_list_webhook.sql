-- Webhook de lista de inventario disponible (tags present=true)

ALTER TABLE inventory_systems
    ADD COLUMN IF NOT EXISTS inventory_list_webhook_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS inventory_list_webhook_url TEXT,
    ADD COLUMN IF NOT EXISTS inventory_list_webhook_secret TEXT;
