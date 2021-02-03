CREATE OR REPLACE FUNCTION crre.region_override_client_order() RETURNS TRIGGER AS $$
BEGIN
    UPDATE crre.order_client_equipment
    SET status= 'IN PROGRESS'
    WHERE order_client_equipment.id = NEW.id_order_client_equipment;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';