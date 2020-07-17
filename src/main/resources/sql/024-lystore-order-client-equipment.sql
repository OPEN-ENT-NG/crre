CREATE TABLE lystore."order-region-equipment"
(
    id bigserial NOT NULL,
    price numeric NOT NULL,
    amount bigint NOT NULL,
    creation_date date NOT NULL,
    modification_date date,
    owner_name character varying NOT NULL,
    owner_id character varying NOT NULL,
    name character varying(255) NOT NULL,
    summary character varying(300),
    description text ,
    image character varying(100) ,
    technical_spec json ,
    status character varying(50),
    id_contract bigint,
    equipment_key bigint NOT NULL,
    id_campaign bigint,
    id_structure character,
    cause_status character varying(300),
    number_validation character varying(50),
    id_order bigint,
    comment text,
    rank numeric,
    id_project bigint,
    id_order_client_equipment bigint,
    id_operation bigint,

    CONSTRAINT order_region_equipment_pkey PRIMARY KEY (id),
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
        REFERENCES lystore.campaign (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_contract_id FOREIGN KEY (id_contract)
        REFERENCES lystore.contract (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_order_id FOREIGN KEY (id_order)
        REFERENCES lystore."order" (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_project_id FOREIGN KEY (id_project)
        REFERENCES lystore.project (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,

    CONSTRAINT fk_operation_id FOREIGN KEY (id_operation)
        REFERENCES lystore.project (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_order_client_id FOREIGN KEY (id_order_client_equipment)
        REFERENCES lystore.order_client_equipment (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT status_values CHECK ((status::text = ANY (ARRAY['WAITING'::character varying::text, 'VALID'::character varying::text, 'WAITING_FOR_ACCEPTANCE'::character varying::text, 'REJECTED'::character varying::text, 'SENT'::character varying::text, 'DONE'::character varying::text]))) NOT VALID,
    CONSTRAINT "Check_price_positive" CHECK (price >= 0::numeric) NOT VALID,
    CONSTRAINT "Check_amount_positive" CHECK (amount::numeric >= 0::numeric) NOT VALID,
    CONSTRAINT "Check_order_or_campaigne_structure" CHECK (id_order_client_equipment is not null or ( id_structure is not null  and id_campaign is not null) ) NOT VALID
)
