-- ID fijo editable del webhook inventario-lista (upsert en la webapp)

ALTER TABLE inventory_systems
    ADD COLUMN IF NOT EXISTS inventory_list_webhook_event_id VARCHAR(64);

UPDATE inventory_systems
SET inventory_list_webhook_event_id = 'evt_00000000800000000000000000000001'
WHERE inventory_list_webhook_event_id IS NULL OR inventory_list_webhook_event_id = '';
