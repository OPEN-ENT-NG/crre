ALTER TABLE lystore.order_client_equipment ADD override_region boolean DEFAULT false;

CREATE FUNCTION lystore.region_override_client_order() RETURNS TRIGGER AS $$
BEGIN
         UPDATE lystore.order_client_equipment
         SET override_region = true,
         id_operation = NEW.id_operation
		 WHERE order_client_equipment.id = NEW.id_order_client_equipment;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER region_override_client_order_trigger AFTER
INSERT
OR
UPDATE ON lystore."order-region-equipment"
FOR EACH ROW WHEN (NEW.id_order_client_equipment IS NOT NULL) EXECUTE PROCEDURE lystore.region_override_client_order();

CREATE FUNCTION lystore.region_delete_order_override_client() RETURNS TRIGGER AS $$
BEGIN
         UPDATE lystore.order_client_equipment
         SET override_region = false,
         id_operation = NULL
		 WHERE order_client_equipment.id = OLD.id_order_client_equipment;
    RETURN OLD;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER region_delete_override_client_order_trigger AFTER
DELETE ON lystore."order-region-equipment"
FOR EACH ROW EXECUTE PROCEDURE lystore.region_delete_order_override_client();