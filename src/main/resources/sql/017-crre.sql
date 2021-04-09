CREATE TABLE crre.order_client_equipment_old (
    id bigserial NOT NULL,
    amount bigint NOT NULL,
    creation_date date NOT NULL DEFAULT CURRENT_DATE,
    id_campaign bigint NOT NULL,
    id_structure character varying NOT NULL,
    status character varying(50) NOT NULL,
    equipment_key VARCHAR,
    equipment_name VARCHAR,
    equipment_image VARCHAR,
    equipment_price numeric,
    equipment_grade VARCHAR,
    equipment_editor VARCHAR,
    equipment_diffusor VARCHAR,
    equipment_format VARCHAR,
    equipment_tva5 double precision,
    equipment_tva20 double precision,
    equipment_priceht double precision,
    cause_status character varying(300),
    comment TEXT,
    user_id character varying(100) NOT NULL,
    id_basket bigint,
    reassort boolean NOT NULL DEFAULT false,
    offers json,
    PRIMARY KEY (id),
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
     REFERENCES crre.campaign (id) MATCH SIMPLE
     ON UPDATE NO ACTION
     ON DELETE NO ACTION,
    CONSTRAINT "Check_amount_positive" CHECK (amount >= 0::numeric),
    CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN PROGRESS', 'WAITING_FOR_ACCEPTANCE', 'REJECTED', 'SENT', 'DONE') ),
    CONSTRAINT fk_basket_order FOREIGN KEY (id_basket) REFERENCES crre.basket_order(id)
    );

CREATE TABLE crre."order-region-equipment-old"
(
    id bigserial NOT NULL,
    amount bigint NOT NULL,
    creation_date date NOT NULL,
    modification_date date,
    owner_name character varying NOT NULL,
    owner_id character varying NOT NULL,
    status character varying(50),
    equipment_key VARCHAR NOT NULL,
    equipment_name VARCHAR,
    equipment_image VARCHAR,
    equipment_price numeric,
    equipment_grade VARCHAR,
    equipment_editor VARCHAR,
    equipment_diffusor VARCHAR,
    equipment_format VARCHAR,
    id_campaign bigint NOT NULL,
    id_structure character varying COLLATE pg_catalog."default" NOT NULL,
    cause_status character varying(300),
    comment text,
    id_project bigint,
    id_order_client_equipment bigint,
    reassort boolean NOT NULL DEFAULT false,

    CONSTRAINT order_region_equipment_old_pkey PRIMARY KEY (id),
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
        REFERENCES crre.campaign (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_order_client_id FOREIGN KEY (id_order_client_equipment)
        REFERENCES crre.order_client_equipment_old (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT constraint_unique_id_order_client_equipment_old UNIQUE (id_order_client_equipment),
    CONSTRAINT "Check_amount_positive" CHECK (amount::numeric >= 0::numeric) NOT VALID,
    CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN PROGRESS', 'WAITING_FOR_ACCEPTANCE', 'REJECTED', 'SENT', 'DONE') )
);

ALTER TABLE crre."order-region-equipment"
DROP CONSTRAINT fk_order_client_id,
ADD CONSTRAINT  fk_order_client_id
FOREIGN KEY (id_order_client_equipment)
REFERENCES crre.order_client_equipment(id)
ON DELETE CASCADE;

DROP TRIGGER region_delete_override_client_order_trigger ON crre."order-region-equipment";