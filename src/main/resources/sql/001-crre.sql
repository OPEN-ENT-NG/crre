CREATE SCHEMA crre;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE crre.scripts (
    filename character varying(255) NOT NULL,
    passed timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT scripts_pkey PRIMARY KEY (filename)
);

CREATE TABLE crre.logs (
    id bigserial NOT NULL,
    date timestamp without time zone DEFAULT now(),
    action character varying(30),
    context character varying(30),
    value json,
    id_user character varying(36),
    username character varying(50),
    item text,
    CONSTRAINT logs_pkey PRIMARY KEY (id)
);

CREATE TABLE crre.structure_group
(
    id bigserial NOT NULL,
    name character varying NOT NULL,
    description text,
    CONSTRAINT structure_group_pkey PRIMARY KEY (id)
);

CREATE TABLE crre.campaign
(
    id bigserial NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    image character varying(100),
    accessible boolean NOT NULL,
    purse_enabled boolean default False,
    priority_enabled boolean DEFAULT true,
    priority_field character varying(50),
    CONSTRAINT campaign_pkey PRIMARY KEY (id)
);

CREATE TABLE crre.rel_group_campaign
(
    id_campaign bigint NOT NULL,
    id_structure_group bigint NOT NULL,
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
        REFERENCES crre.campaign (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_structure_group FOREIGN KEY (id_structure_group)
        REFERENCES crre.structure_group (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE crre.rel_group_structure
(
    id_structure character varying(50) NOT NULL,
    id_structure_group bigint NOT NULL,
    CONSTRAINT rel_structure_group_pkey PRIMARY KEY (id_structure, id_structure_group),
    CONSTRAINT fk_id_structure_group FOREIGN KEY (id_structure_group)
        REFERENCES crre.structure_group (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE crre.purse(
    id bigserial NOT NULL,
    id_structure character varying(36),
    amount numeric,
    id_campaign bigint,
    initial_amount NUMERIC,
    CONSTRAINT purse_pkey PRIMARY KEY (id),
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
       REFERENCES crre.campaign (id) MATCH SIMPLE
       ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT purse_id_structure_id_campaign_key UNIQUE (id_structure, id_campaign),
    CONSTRAINT "Check_amount_positive" CHECK (amount >= 0::numeric)
);

CREATE TABLE crre.licences
(
    id_purse bigint NOT NULL,
    name character varying(36),
    amount bigint,
    initial_amount bigint,
    id bigint NOT NULL,
    CONSTRAINT licences_pkey PRIMARY KEY (id),
    CONSTRAINT fk_purse_id FOREIGN KEY (id_purse)
        REFERENCES crre.purse (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
);

CREATE TABLE crre.basket_equipment (
    id bigserial NOT NULL,
    amount integer NOT NULL,
    processing_date date,
    id_equipment bigint NOT NULL,
    id_campaign bigint NOT NULL,
    id_structure character varying NOT NULL,
    "comment" TEXT,
    price_proposal numeric,
    id_type bigint DEFAULT 1,
    owner_id character varying,
    owner_name character varying(255),
    reassort boolean NOT NULL DEFAULT false,
    PRIMARY KEY (id),
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
       REFERENCES crre.campaign (id) MATCH SIMPLE
       ON UPDATE NO ACTION
       ON DELETE NO ACTION,
    CONSTRAINT "Check_amount_positive" CHECK (amount >= 0::numeric)
);


CREATE TABLE crre.order_client_equipment (
    id bigserial NOT NULL,
    amount bigint NOT NULL,
    creation_date date NOT NULL DEFAULT CURRENT_DATE,
    id_campaign bigint NOT NULL,
    id_structure character varying NOT NULL,
    status character varying(50) NOT NULL,
    equipment_key bigint,
    cause_status character varying(300),
    comment TEXT,
    user_id character varying(100) NOT NULL,
    id_basket bigint,
    reassort boolean NOT NULL DEFAULT false,
    PRIMARY KEY (id),
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
     REFERENCES crre.campaign (id) MATCH SIMPLE
     ON UPDATE NO ACTION
     ON DELETE NO ACTION,
    CONSTRAINT "Check_amount_positive" CHECK (amount >= 0::numeric),
    CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN PROGRESS', 'WAITING_FOR_ACCEPTANCE', 'REJECTED', 'SENT', 'DONE') ),
    CONSTRAINT fk_basket_order FOREIGN KEY (id_basket) REFERENCES crre.basket_order(id)
);

CREATE INDEX fki_fk_basket_id ON crre.order_client_equipment USING btree (id_basket);

CREATE TABLE crre.order_file (
    id character varying (36) NOT NULL,
    id_order_client_equipment bigint NOT NULL,
    filename character varying (255) NOT NULL,
    CONSTRAINT order_file_pkey PRIMARY KEY (id, id_order_client_equipment),
    CONSTRAINT fk_order_client_equipment_id FOREIGN KEY (id_order_client_equipment)
        REFERENCES crre.order_client_equipment (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE crre."order-region-equipment"
(
    id bigserial NOT NULL,
    amount bigint NOT NULL,
    creation_date date NOT NULL,
    modification_date date,
    owner_name character varying NOT NULL,
    owner_id character varying NOT NULL,
    status character varying(50),
    equipment_key bigint NOT NULL,
    id_campaign bigint NOT NULL,
    id_structure character varying COLLATE pg_catalog."default" NOT NULL,
    cause_status character varying(300),
    comment text,
    id_project bigint,
    id_order_client_equipment bigint,
    reassort boolean NOT NULL DEFAULT false,

    CONSTRAINT order_region_equipment_pkey PRIMARY KEY (id),
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
        REFERENCES crre.campaign (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_order_client_id FOREIGN KEY (id_order_client_equipment)
        REFERENCES crre.order_client_equipment (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT constraint_unique_id_order_client_equipment UNIQUE (id_order_client_equipment),
    CONSTRAINT "Check_amount_positive" CHECK (amount::numeric >= 0::numeric) NOT VALID,
    CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN PROGRESS', 'WAITING_FOR_ACCEPTANCE', 'REJECTED', 'SENT', 'DONE') )
);

CREATE TABLE crre.project (
    id integer NOT NULL,
    title character varying(50),

    CONSTRAINT project_pkey PRIMARY KEY (id)
);

CREATE TABLE crre.basket_order (
    id integer NOT NULL,
    name character varying(255),
    id_structure character varying(255),
    id_campaign bigint,
    name_user character varying(255),
    id_user character varying(255),
    total double precision,
    amount bigint,
    created date,

    CONSTRAINT basket_order_pkey PRIMARY KEY (id)
);

CREATE TABLE crre.students
(
    "id_structure" character varying(50),
    "Seconde" bigint DEFAULT 0,
    "Premiere" bigint DEFAULT 0,
    "Terminale" bigint DEFAULT 0,
    "pro" boolean,
    PRIMARY KEY (id_structure),
    CONSTRAINT structure_unique UNIQUE (id_structure)
);