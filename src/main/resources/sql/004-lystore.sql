ALTER TABLE lystore.contract DROP COLUMN id_program;

ALTER TABLE lystore.order_client_equipment DROP COLUMN file;
ALTER TABLE lystore.order_client_equipment ADD COLUMN id_order bigint;

CREATE TABLE lystore.order (
  id bigserial NOT NULL,
  engagement_number character varying(255),
  label_program character varying(255),
  date_creation timestamp without time zone NOT NULL DEFAULT now(),
  order_number character varying (255),
  CONSTRAINT order_pkey PRIMARY KEY (id)
);

CREATE TABLE lystore.file (
  id bigserial NOT NULL,
  id_mongo character varying(255),
  owner character varying(255),
  date timestamp without time zone NOT NULL DEFAULT now(),
  id_order bigint,
  CONSTRAINT file_pkey PRIMARY KEY (id),
  CONSTRAINT fk_order_id FOREIGN KEY (id_order)
      REFERENCES lystore.order (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
);

ALTER TABLE lystore.order_client_equipment ADD CONSTRAINT fk_order_id FOREIGN KEY (id_order)
      REFERENCES lystore.order (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE;