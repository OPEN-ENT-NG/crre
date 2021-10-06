ALTER TABLE crre."order-region-equipment"
ALTER COLUMN cause_status TYPE text;

ALTER TABLE crre.order_client_equipment
ALTER COLUMN cause_status TYPE text;

ALTER TABLE crre."order-region-equipment-old"
ALTER COLUMN cause_status TYPE text;

ALTER TABLE crre.order_client_equipment_old
ALTER COLUMN cause_status TYPE text;