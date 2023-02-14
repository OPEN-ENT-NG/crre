ALTER TABLE crre."order-region-equipment-old" DROP CONSTRAINT "status_values";
ALTER TABLE crre."order-region-equipment" DROP CONSTRAINT "status_values";
ALTER TABLE crre.order_client_equipment_old DROP CONSTRAINT "status_values";
ALTER TABLE crre.order_client_equipment DROP CONSTRAINT "status_values";

UPDATE crre."order-region-equipment-old" SET status = 'IN_PROGRESS' WHERE status = 'IN PROGRESS';
UPDATE crre."order-region-equipment" SET status = 'IN_PROGRESS' WHERE status = 'IN PROGRESS';
UPDATE crre.order_client_equipment_old SET status = 'IN_PROGRESS' WHERE status = 'IN PROGRESS';
UPDATE crre.order_client_equipment SET status = 'IN_PROGRESS' WHERE status = 'IN PROGRESS';

ALTER TABLE crre."order-region-equipment-old"
    add CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN_PROGRESS', 'WAITING_FOR_ACCEPTANCE',
                                                     'REJECTED', 'SENT', 'DONE', 'RESUBMIT') );

ALTER TABLE crre."order-region-equipment"
    add CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN_PROGRESS', 'WAITING_FOR_ACCEPTANCE',
                                                     'REJECTED', 'SENT', 'DONE', 'RESUBMIT') );

ALTER TABLE crre.order_client_equipment_old
    add CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN_PROGRESS', 'WAITING_FOR_ACCEPTANCE',
                                                     'REJECTED', 'SENT', 'DONE', 'RESUBMIT') );

ALTER TABLE crre.order_client_equipment
    add CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN_PROGRESS', 'WAITING_FOR_ACCEPTANCE',
                                                     'REJECTED', 'SENT', 'DONE', 'RESUBMIT') );

CREATE OR REPLACE FUNCTION crre.region_override_client_order() RETURNS TRIGGER AS $$
BEGIN
    UPDATE crre.order_client_equipment
    SET status= 'IN_PROGRESS'
    WHERE order_client_equipment.id = NEW.id_order_client_equipment;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';