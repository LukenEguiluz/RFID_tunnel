-- 3 ciclos consecutivos sin lectura → removed del inventario (webhook)

UPDATE inventory_systems
SET cycles_to_lost = 3
WHERE cycles_to_lost IS NULL OR cycles_to_lost < 1 OR cycles_to_lost > 3;
