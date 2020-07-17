ALTER TABLE lystore."order-region-equipment" DROP CONSTRAINT "Check_order_or_campaigne_structure";
ALTER TABLE lystore."order-region-equipment" ALTER COLUMN id_structure SET NOT NULL;
ALTER TABLE lystore."order-region-equipment" ALTER COLUMN id_campaign SET NOT NULL;
ALTER TABLE lystore."order-region-equipment" ADD CONSTRAINT constraint_unique_id_order_client_equipment UNIQUE (id_order_client_equipment);
