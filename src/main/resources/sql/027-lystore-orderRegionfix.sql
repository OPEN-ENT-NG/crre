ALTER table lystore."order-region-equipment"
DROP CONSTRAINT fk_operation_id,
ADD CONSTRAINT fk_operation_id FOREIGN KEY (id_operation)
        REFERENCES lystore.operation (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION