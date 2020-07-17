CREATE SCHEMA lystore;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE lystore.scripts (
  filename character varying(255) NOT NULL,
  passed timestamp without time zone NOT NULL DEFAULT now(),
  CONSTRAINT scripts_pkey PRIMARY KEY (filename)
);

CREATE TABLE lystore.agent (
  id bigserial NOT NULL,
  email character varying(255),
  department character varying(255),
  name character varying(100),
  phone character varying(45),
  CONSTRAINT agent_pkey PRIMARY KEY (id)
);

CREATE TABLE lystore.supplier (
  id bigserial NOT NULL,
  name character varying(100),
  address character varying(255),
  email character varying(255),
  phone character varying(45),
  CONSTRAINT supplier_pkey PRIMARY KEY (id)
);

CREATE TABLE lystore.contract_type (
  id bigserial NOT NULL,
  code character varying(50),
  name character varying(255),
  CONSTRAINT contract_type_pkey PRIMARY KEY (id)
);

CREATE TABLE lystore.program (
  id bigserial NOT NULL,
  name character varying(255),
  CONSTRAINT program_pkey PRIMARY KEY (id)
);

CREATE TABLE lystore.contract (
  id bigserial NOT NULL,
  name character varying(255),
  annual_min numeric,
  annual_max numeric,
  start_date date,
  nb_renewal numeric,
  id_contract_type bigint,
  max_brink numeric,
  id_supplier bigint,
  id_agent bigint,
  id_program bigint,
  reference character varying(50),
  end_date date,
  renewal_end date,
  CONSTRAINT contract_pk PRIMARY KEY (id),
  CONSTRAINT fk_agent_id FOREIGN KEY (id_agent)
  REFERENCES lystore.agent (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_contract_type_id FOREIGN KEY (id_contract_type)
  REFERENCES lystore.contract_type (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_program_id FOREIGN KEY (id_program)
  REFERENCES lystore.program (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_supplier_id FOREIGN KEY (id_supplier)
  REFERENCES lystore.supplier (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT "Check_annual_min_positive" CHECK (annual_min >= 0::numeric),
  CONSTRAINT "Check_annual_max_positive" CHECK (annual_max >= 0::numeric),
  CONSTRAINT "Check_annual_max_max" CHECK (annual_max >= annual_min::numeric)

);

CREATE TABLE lystore.tag (
  id bigserial NOT NULL,
  name character varying(255),
  color character varying(7),
  CONSTRAINT tag_pkey PRIMARY KEY (id)
);

CREATE TABLE lystore.tax (
  id bigserial NOT NULL,
  name character varying(255),
  value numeric,
  CONSTRAINT tax_pkey PRIMARY KEY (id),
  CONSTRAINT "Check_tax_value" CHECK (value >= 0::numeric)
);

CREATE TABLE lystore.equipment (
  id bigserial NOT NULL,
  name character varying(255) NOT NULL,
  summary character varying(300),
  description text,
  price numeric NOT NULL,
  id_tax bigint NOT NULL,
  image character varying(100),
  id_contract bigint NOT NULL,
  status character varying(50),
  technical_specs json,
  CONSTRAINT equipment_pkey PRIMARY KEY (id),
  CONSTRAINT fk_contract_id FOREIGN KEY (id_contract)
  REFERENCES lystore.contract (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE ,
  CONSTRAINT fk_tax_id FOREIGN KEY (id_tax)
  REFERENCES lystore.tax (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT "Check_price_positive" CHECK (price >= 0::numeric)
);

CREATE TABLE lystore.rel_equipment_tag (
  id_equipment bigint,
  id_tag bigint,
  CONSTRAINT fk_equipment_id FOREIGN KEY (id_equipment)
  REFERENCES lystore.equipment (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_tag_id FOREIGN KEY (id_tag)
  REFERENCES lystore.tag (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE lystore.equipment_option (
  id bigserial NOT NULL,
  name character varying(100) NOT NULL,
  price numeric NOT NULL,
  amount integer NOT NULL,
  required boolean NOT NULL,
  id_tax bigint NOT NULL,
  id_equipment bigint NOT NULL,
  CONSTRAINT id PRIMARY KEY (id),
  CONSTRAINT fk_equipment_id FOREIGN KEY (id_equipment)
  REFERENCES lystore.equipment (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_tax_id FOREIGN KEY (id_tax)
  REFERENCES lystore.tax (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "Check_price_positive" CHECK (price >= 0::numeric),
  CONSTRAINT "Check_amount_positive" CHECK (amount >= 0::numeric)
);

CREATE TABLE lystore.logs (
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

	CREATE TABLE lystore.structure_group
(
    id bigserial NOT NULL,
    name character varying NOT NULL,
    description text,
    CONSTRAINT structure_group_pkey PRIMARY KEY (id)
);

	CREATE TABLE lystore.campaign
(
    id bigserial NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    image character varying(100),
    accessible boolean NOT NULL,
    CONSTRAINT campaign_pkey PRIMARY KEY (id)
);

CREATE TABLE lystore.rel_group_campaign
(
    id_campaign bigint NOT NULL,
    id_structure_group bigint NOT NULL,
    id_tag bigint NOT NULL,
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
        REFERENCES lystore.campaign (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_structure_group FOREIGN KEY (id_structure_group)
        REFERENCES lystore.structure_group (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_tag_id FOREIGN KEY (id_tag)
        REFERENCES lystore.tag (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE lystore.rel_group_structure
(
    id_structure character varying(50) NOT NULL,
    id_structure_group bigint NOT NULL,
    CONSTRAINT rel_structure_group_pkey PRIMARY KEY (id_structure, id_structure_group),
    CONSTRAINT fk_id_structure_group FOREIGN KEY (id_structure_group)
        REFERENCES lystore.structure_group (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE lystore.purse(
  id bigserial NOT NULL,
  id_structure character varying(36),
  amount numeric,
  id_campaign bigint,
  CONSTRAINT purse_pkey PRIMARY KEY (id),
  CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
  REFERENCES lystore.campaign (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT purse_id_structure_id_campaign_key UNIQUE (id_structure, id_campaign),
  CONSTRAINT "Check_amount_positive" CHECK (amount >= 0::numeric)
);

CREATE TABLE lystore.basket_equipment (
    id bigserial NOT NULL,
    amount integer NOT NULL,
    processing_date date,
    id_equipment bigint NOT NULL,
    id_campaign bigint NOT NULL,
    id_structure character varying NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_equipment_id FOREIGN KEY (id_equipment)
        REFERENCES lystore.equipment (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
        REFERENCES lystore.campaign (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT "Check_amount_positive" CHECK (amount >= 0::numeric)
);

CREATE TABLE lystore.basket_option (
    id bigserial NOT NULL,
    id_basket_equipment bigint,
    id_option bigint,
    PRIMARY KEY (id),
    CONSTRAINT fk_basket_equipment_id FOREIGN KEY (id_basket_equipment)
        REFERENCES lystore.basket_equipment (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_option_id FOREIGN KEY (id_option)
        REFERENCES lystore.equipment_option (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);
CREATE TABLE lystore.order_client_equipment (
    id bigserial NOT NULL,
    price numeric NOT NULL,
    tax_amount numeric NOT NULL,
    amount bigint NOT NULL,
    creation_date date NOT NULL DEFAULT CURRENT_DATE,
    id_campaign bigint NOT NULL,
    id_equipment bigint NOT NULL,
    id_structure character varying NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_equipment_id FOREIGN KEY (id_equipment)
        REFERENCES lystore.equipment (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
        REFERENCES lystore.campaign (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT "Check_price_positive" CHECK (price >= 0::numeric),
    CONSTRAINT "Check_amount_positive" CHECK (amount >= 0::numeric)
);

CREATE TABLE lystore.order_client_options (
    id bigserial NOT NULL,
    tax_amount numeric,
    price numeric,
    id_order_client_equipment bigint,
    id_option bigint,
    CONSTRAINT "Pk_id_ordet_client_option" PRIMARY KEY (id),
    CONSTRAINT "FK_order-client-equipment_id" FOREIGN KEY (id_order_client_equipment)
        REFERENCES lystore.order_client_equipment (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT "FK_options_id" FOREIGN KEY (id_option)
        REFERENCES lystore.equipment_option (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT "Check_price_positive" CHECK (price >= 0::numeric),
    CONSTRAINT "Check_tax_amount_positive" CHECK (tax_amount >= 0::numeric)
);

