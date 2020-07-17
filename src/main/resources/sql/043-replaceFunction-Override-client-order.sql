CREATE OR REPLACE FUNCTION lystore.region_override_client_order() RETURNS TRIGGER AS $$
BEGIN
         UPDATE lystore.order_client_equipment
         SET override_region = true,
		 status= 'IN PROGRESS',
         id_operation = NEW.id_operation
		 WHERE order_client_equipment.id = NEW.id_order_client_equipment;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';
