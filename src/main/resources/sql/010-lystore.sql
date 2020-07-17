CREATE TABLE lystore.equipment_type (
  id bigserial NOT NULL,
  name character varying(255) NOT NULL,
  CONSTRAINT equipment_type_pkey PRIMARY KEY (id)
);

INSERT INTO lystore.equipment_type
	VALUES (1,'EQUIPEMENT');
INSERT INTO lystore.equipment_type
	VALUES (2,'PRESTATION');


ALTER TABLE lystore.equipment
  ADD COLUMN id_type bigint;
ALTER TABLE lystore.equipment
  ADD COLUMN option_enabled boolean NOT NULL DEFAULT false;

UPDATE lystore.equipment SET id_type = 1 WHERE id_type IS NULL;

ALTER TABLE lystore.equipment
ALTER COLUMN id_type SET NOT NULL;

ALTER TABLE lystore.equipment
  ADD CONSTRAINT fk_equipment_type_id FOREIGN KEY (id_type)
	REFERENCES lystore.equipment_type (id) MATCH SIMPLE
	ON UPDATE NO ACTION ON DELETE CASCADE;


ALTER TABLE lystore.campaign
ADD COLUMN purse_enabled boolean;

UPDATE lystore.campaign SET purse_enabled = true;

ALTER TABLE lystore.campaign
ALTER COLUMN purse_enabled SET DEFAULT FALSE;