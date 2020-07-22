CREATE SCHEMA crre;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE crre.scripts (
  filename character varying(255) NOT NULL,
  passed timestamp without time zone NOT NULL DEFAULT now(),
  CONSTRAINT scripts_pkey PRIMARY KEY (filename)
);

CREATE TABLE crre.agent (
  id bigserial NOT NULL,
  email character varying(255),
  department character varying(255),
  name character varying(100),
  phone character varying(45),
  CONSTRAINT agent_pkey PRIMARY KEY (id)
);

CREATE TABLE crre.supplier (
  id bigserial NOT NULL,
  name character varying(100),
  address character varying(255),
  email character varying(255),
  phone character varying(45),
  CONSTRAINT supplier_pkey PRIMARY KEY (id)
);

CREATE TABLE crre.contract_type (
  id bigserial NOT NULL,
  code character varying(50),
  name character varying(255),
  description text,
  CONSTRAINT contract_type_pkey PRIMARY KEY (id)
);

CREATE TABLE crre.contract (
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
  reference character varying(50),
  end_date date,
  renewal_end date,
  file boolean NOT NULL DEFAULT false,
  CONSTRAINT contract_pk PRIMARY KEY (id),
  CONSTRAINT fk_agent_id FOREIGN KEY (id_agent)
  REFERENCES crre.agent (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_contract_type_id FOREIGN KEY (id_contract_type)
  REFERENCES crre.contract_type (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_supplier_id FOREIGN KEY (id_supplier)
  REFERENCES crre.supplier (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT "Check_annual_min_positive" CHECK (annual_min >= 0::numeric),
  CONSTRAINT "Check_annual_max_positive" CHECK (annual_max >= 0::numeric),
  CONSTRAINT "Check_annual_max_max" CHECK (annual_max >= annual_min::numeric)

);

CREATE TABLE crre.tax (
  id bigserial NOT NULL,
  name character varying(255),
  value numeric,
  CONSTRAINT tax_pkey PRIMARY KEY (id),
  CONSTRAINT "Check_tax_value" CHECK (value >= 0::numeric)
);

CREATE TABLE crre.equipment (
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
  id_type bigint NOT NULL,
  option_enabled boolean NOT NULL DEFAULT false,
  reference character varying(50),
  price_editable boolean NOT NULL DEFAULT false,
  CONSTRAINT equipment_pkey PRIMARY KEY (id),
  CONSTRAINT fk_contract_id FOREIGN KEY (id_contract)
  REFERENCES crre.contract (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE ,
  CONSTRAINT fk_tax_id FOREIGN KEY (id_tax)
  REFERENCES crre.tax (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT "Check_price_positive" CHECK (price >= 0::numeric)
);

CREATE TABLE crre.equipment_option (
  id bigserial NOT NULL,
  amount integer NOT NULL,
  required boolean NOT NULL,
  id_equipment bigint NOT NULL,
  id_option bigint NOT NULL,
  reference character varying(255),
  warranty bigint NOT NULL,
  catalog_enabled boolean NOT NULL DEFAULT true,
  CONSTRAINT id PRIMARY KEY (id),
  CONSTRAINT fk_equipment_id FOREIGN KEY (id_equipment)
  REFERENCES crre.equipment (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_equipment_option_id FOREIGN KEY (id_option)
      REFERENCES crre.equipment (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT "Check_amount_positive" CHECK (amount >= 0::numeric)
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

CREATE TABLE crre.basket_equipment (
    id bigserial NOT NULL,
    amount integer NOT NULL,
    processing_date date,
    id_equipment bigint NOT NULL,
    id_campaign bigint NOT NULL,
    id_structure character varying NOT NULL,
    "comment" TEXT,
    price_proposal numeric,
    id_type bigint NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_equipment_id FOREIGN KEY (id_equipment)
        REFERENCES crre.equipment (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
        REFERENCES crre.campaign (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT "Check_amount_positive" CHECK (amount >= 0::numeric)
);

CREATE TABLE crre.basket_option (
    id bigserial NOT NULL,
    id_basket_equipment bigint,
    id_option bigint,
    PRIMARY KEY (id),
    CONSTRAINT fk_basket_equipment_id FOREIGN KEY (id_basket_equipment)
        REFERENCES crre.basket_equipment (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_option_id FOREIGN KEY (id_option)
        REFERENCES crre.equipment_option (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

CREATE TABLE crre.order (
    id bigserial NOT NULL,
    engagement_number character varying(255),
    date_creation timestamp without time zone NOT NULL DEFAULT now(),
    order_number character varying (255),
    CONSTRAINT order_pkey PRIMARY KEY (id)
);

CREATE TABLE crre.title (
    id bigserial NOT NULL,
    name character varying(255),
    CONSTRAINT title_pkey PRIMARY KEY (id)
);

CREATE TABLE crre.grade (
                            id bigserial NOT NULL,
                            name character varying(255),
                            CONSTRAINT grade_pkey PRIMARY KEY (id)
);

CREATE TABLE crre.project (
    id bigserial NOT NULL,
    description character varying(255),
    id_title bigint NOT NULL,
    id_grade bigint,
    building character varying(255),
    stair integer,
    room character varying(50),
    site character varying(255),
    preference bigint,
    CONSTRAINT project_pkey PRIMARY KEY (id),
    CONSTRAINT fk_title_id FOREIGN KEY (id_title)
      REFERENCES crre.title (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_grade_id FOREIGN KEY (id_grade)
      REFERENCES crre.grade (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE crre.order_client_equipment (
    id bigserial NOT NULL,
    price numeric NOT NULL,
    tax_amount numeric NOT NULL,
    amount bigint NOT NULL,
    creation_date date NOT NULL DEFAULT CURRENT_DATE,
    id_campaign bigint NOT NULL,
    id_structure character varying NOT NULL,
    name character varying(255) NOT NULL,
    summary character varying(300),
    description text,
    image character varying(100),
    technical_spec json,
    status character varying(50) NOT NULL,
    id_contract bigint  NOT NULL,
    equipment_key bigint,
    cause_status character varying(300),
    "number_validation" character varying(50),
    id_order bigint,
    "comment" TEXT,
    price_proposal numeric,
    id_project bigint,
    rank numeric,
    override_region boolean DEFAULT false,
    id_type bigint NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
        REFERENCES crre.campaign (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_contract_id FOREIGN KEY (id_contract)
        REFERENCES crre.contract (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_order_id FOREIGN KEY (id_order)
        REFERENCES crre.order (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_project_id  FOREIGN KEY (id_project)
        REFERENCES crre.project (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "Check_price_positive" CHECK (price >= 0::numeric),
    CONSTRAINT "Check_amount_positive" CHECK (amount >= 0::numeric),
    CONSTRAINT "Check_tax_amount_positive" CHECK (tax_amount >= 0::numeric),
    CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN PROGRESS', 'WAITING_FOR_ACCEPTANCE', 'REJECTED', 'SENT', 'DONE') )
);

CREATE TABLE crre.order_client_options (
    id bigserial NOT NULL,
    tax_amount numeric,
    price numeric,
    id_order_client_equipment bigint,
    name character varying NOT NULL,
    amount integer NOT NULL,
    required boolean NOT NULL,
    id_type bigint NOT NULL DEFAULT 1,
    CONSTRAINT "Pk_id_ordet_client_option" PRIMARY KEY (id),
    CONSTRAINT "Check_price_positive" CHECK (price >= 0::numeric),
    CONSTRAINT "Check_tax_amount_positive" CHECK (tax_amount >= 0::numeric)
);

CREATE TABLE crre.file (
    id bigserial NOT NULL,
    id_mongo character varying(255),
    owner character varying(255),
    date timestamp without time zone NOT NULL DEFAULT now(),
    id_order bigint,
    CONSTRAINT file_pkey PRIMARY KEY (id),
    CONSTRAINT fk_order_id FOREIGN KEY (id_order)
      REFERENCES crre.order (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE crre.basket_file (
    id character varying (36) NOT NULL,
    id_basket_equipment bigint NOT NULL,
    filename character varying (255) NOT NULL,
    CONSTRAINT basket_file_pkey PRIMARY KEY (id, id_basket_equipment),
    CONSTRAINT fk_basket_equipment_id FOREIGN KEY (id_basket_equipment)
     REFERENCES crre.basket_equipment (id) MATCH SIMPLE
     ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE crre.order_file (
    id character varying (36) NOT NULL,
    id_order_client_equipment bigint NOT NULL,
    filename character varying (255) NOT NULL,
    CONSTRAINT order_file_pkey PRIMARY KEY (id, id_order_client_equipment),
    CONSTRAINT fk_order_client_equipment_id FOREIGN KEY (id_order_client_equipment)
        REFERENCES crre.order_client_equipment (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE crre.rel_title_campaign_structure (
    id_title bigint NOT NULL,
    id_campaign bigint NOT NULL,
    id_structure character varying (36),
    CONSTRAINT rel_title_campaign_structure_pkey PRIMARY KEY (id_title, id_campaign, id_structure),
    CONSTRAINT fk_title_id FOREIGN KEY (id_title) REFERENCES crre.title (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign) REFERENCES crre.campaign (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE crre."order-region-equipment"
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
    id_campaign bigint NOT NULL,
    id_structure character varying COLLATE pg_catalog."default" NOT NULL,
    cause_status character varying(300),
    number_validation character varying(50),
    id_order bigint,
    comment text,
    rank numeric,
    id_project bigint,
    id_order_client_equipment bigint,

    CONSTRAINT order_region_equipment_pkey PRIMARY KEY (id),
    CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign)
        REFERENCES crre.campaign (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_contract_id FOREIGN KEY (id_contract)
        REFERENCES crre.contract (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_order_id FOREIGN KEY (id_order)
        REFERENCES crre."order" (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_project_id FOREIGN KEY (id_project)
        REFERENCES crre.project (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT fk_order_client_id FOREIGN KEY (id_order_client_equipment)
        REFERENCES crre.order_client_equipment (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT constraint_unique_id_order_client_equipment UNIQUE (id_order_client_equipment),
    CONSTRAINT "Check_price_positive" CHECK (price >= 0::numeric) NOT VALID,
    CONSTRAINT "Check_amount_positive" CHECK (amount::numeric >= 0::numeric) NOT VALID,
    CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN PROGRESS', 'WAITING_FOR_ACCEPTANCE', 'REJECTED', 'SENT', 'DONE') )
);


CREATE  SEQUENCE crre.seq_validation_number
    INCREMENT 1
    MINVALUE 0
    MAXVALUE 99999999999999
    START 19700101001
    CACHE 1;

CREATE OR REPLACE FUNCTION crre.get_validation_number()
    RETURNS VARCHAR AS $$
DECLARE
    nextSeqVal VARCHAR ;
    valeurinitiale VARCHAR := '0001';
BEGIN
    select  pg_catalog.nextval('crre.seq_validation_number'::regclass) into nextSeqVal;
    if (left(nextSeqVal ,8 ) != replace (  CURRENT_DATE || '', '-' , ''))
    then
        select replace (  CURRENT_DATE || '', '-' , '') || valeurinitiale into nextSeqVal;
        PERFORM pg_catalog.setval('crre.seq_validation_number'::regclass, nextSeqVal::BIGINT, true);
    end if;
    return nextSeqVal;
END;
$$ LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION crre.create_equipment_reference()
    RETURNS trigger AS $BODY$
BEGIN
    IF NEW.reference IS NULL THEN
        UPDATE crre.equipment SET reference = ('REF#' || NEW.id) WHERE id = NEW.id;
    END IF;
    RETURN NEW;
END;
$BODY$
    LANGUAGE plpgsql VOLATILE;

CREATE TRIGGER on_equipment_insert
    AFTER INSERT  ON crre.equipment
    FOR EACH ROW
    EXECUTE PROCEDURE crre.create_equipment_reference();

CREATE OR REPLACE FUNCTION delete_empty_project()
    RETURNS trigger AS  $$
BEGIN
    DELETE FROM crre.project as prj
    WHERE prj.id NOT IN (SELECT pp.id FROM crre.project as pp
                                               INNER JOIN crre.order_client_equipment oce
                                                          ON oce.id_project = pp.id
                         GROUP BY pp.id)
      AND prj.id NOT IN (SELECT pp.id FROM crre.project as pp
                                               INNER JOIN crre."order-region-equipment" ore
                                                          ON ore.id_project = pp.id
                         GROUP BY pp.id)
    ;
    RETURN NULL;
END;
$$  LANGUAGE plpgsql;

CREATE TRIGGER check_empty_project AFTER DELETE
    ON crre.order_client_equipment
    FOR EACH ROW
    EXECUTE PROCEDURE delete_empty_project();

CREATE OR REPLACE FUNCTION crre.region_override_client_order() RETURNS TRIGGER AS $$
BEGIN
    UPDATE crre.order_client_equipment
    SET override_region = true,
        status= 'IN PROGRESS'
    WHERE order_client_equipment.id = NEW.id_order_client_equipment;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER region_override_client_order_trigger AFTER
    INSERT
    OR
    UPDATE ON crre."order-region-equipment"
    FOR EACH ROW WHEN (NEW.id_order_client_equipment IS NOT NULL) EXECUTE PROCEDURE crre.region_override_client_order();

CREATE FUNCTION crre.region_delete_order_override_client() RETURNS TRIGGER AS $$
BEGIN
    UPDATE crre.order_client_equipment
    SET override_region = false
    WHERE order_client_equipment.id = OLD.id_order_client_equipment;
    RETURN OLD;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER region_delete_override_client_order_trigger AFTER
    DELETE ON crre."order-region-equipment"
    FOR EACH ROW EXECUTE PROCEDURE crre.region_delete_order_override_client();

CREATE OR REPLACE FUNCTION order_without_ref()
    RETURNS trigger AS  $$
BEGIN
    DELETE FROM crre.order
    WHERE id NOT IN (
        SELECT DISTINCT id_order FROM crre.order_client_equipment
        WHERE id_order IS NOT NULL
        ORDER BY id_order );
    RETURN NULL;
END;
$$  LANGUAGE plpgsql;

CREATE TRIGGER check_order_no_ref AFTER UPDATE
    ON crre.order_client_equipment
    FOR EACH ROW
EXECUTE PROCEDURE order_without_ref();

CREATE INDEX equipment_idcontract_name_reference_idx ON crre.equipment USING btree (id_contract, reference, name);
