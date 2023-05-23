ALTER TABLE crre."order-region-equipment-old" DROP CONSTRAINT "status_values";
ALTER TABLE crre."order-region-equipment" DROP CONSTRAINT "status_values";
ALTER TABLE crre.order_client_equipment_old DROP CONSTRAINT "status_values";
ALTER TABLE crre.order_client_equipment DROP CONSTRAINT "status_values";

ALTER TABLE crre."order-region-equipment-old"
    add CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN_PROGRESS', 'WAITING_FOR_ACCEPTANCE',
                                                     'REJECTED', 'SENT', 'DONE', 'RESUBMIT', 'ARCHIVED') );

ALTER TABLE crre."order-region-equipment"
    add CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN_PROGRESS', 'WAITING_FOR_ACCEPTANCE',
                                                     'REJECTED', 'SENT', 'DONE', 'RESUBMIT', 'ARCHIVED') );

ALTER TABLE crre.order_client_equipment_old
    add CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN_PROGRESS', 'WAITING_FOR_ACCEPTANCE',
                                                     'REJECTED', 'SENT', 'DONE', 'RESUBMIT', 'ARCHIVED') );

ALTER TABLE crre.order_client_equipment
    add CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN_PROGRESS', 'WAITING_FOR_ACCEPTANCE',
                                                     'REJECTED', 'SENT', 'DONE', 'RESUBMIT', 'ARCHIVED') );