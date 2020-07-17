ALTER TABLE lystore.order_client_equipment DROP CONSTRAINT fk_equipment_id;

ALTER TABLE lystore.order_client_equipment DROP COLUMN id_equipment;

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN name character varying(255) NOT NULL;

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN summary character varying(300);

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN description text;

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN image character varying(100);

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN technical_spec json;

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN status character varying(50) NOT NULL;

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN id_contract bigint  NOT NULL;
	ALTER TABLE lystore.order_client_equipment
    ADD COLUMN equipment_key bigint;

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN cause_status character varying(300);

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN "number" character varying(50);

ALTER TABLE lystore.order_client_equipment
    ADD CONSTRAINT fk_contract_id FOREIGN KEY (id_contract)
    REFERENCES lystore.contract (id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION;

ALTER TABLE lystore.order_client_equipment
    ADD CONSTRAINT "Check_tax_amount_positive" CHECK (tax_amount >= 0::numeric);

ALTER TABLE lystore.order_client_options DROP COLUMN id_option;

ALTER TABLE lystore.order_client_options
    ADD COLUMN name character varying NOT NULL;

ALTER TABLE lystore.order_client_options
    ADD COLUMN amount integer NOT NULL;

ALTER TABLE lystore.order_client_equipment
    ADD CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID', 'REJECTED', 'SENT', 'DONE') );

ALTER TABLE lystore.order_client_options
    ADD COLUMN required boolean NOT NULL;
ALTER TABLE lystore.order_client_options DROP CONSTRAINT "FK_order-client-equipment_id";