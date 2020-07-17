CREATE TABLE lystore.label_operation
(
    id bigserial,
    label character varying(50),
    PRIMARY KEY (id),
    CONSTRAINT uq_label UNIQUE (label)
);


CREATE TABLE lystore.operation
(
    id bigserial,
    id_label bigint,
    status character varying(50),
    id_instruction bigint,
    PRIMARY KEY (id),
    CONSTRAINT fk_id_label FOREIGN KEY (id_label)
        REFERENCES lystore.label_operation (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

ALTER TABLE lystore.order_client_equipment
    ADD COLUMN id_operation bigint;
ALTER TABLE lystore.order_client_equipment
    ADD CONSTRAINT fk_operation_id FOREIGN KEY (id_operation)
    REFERENCES lystore.operation (id) MATCH SIMPLE;


