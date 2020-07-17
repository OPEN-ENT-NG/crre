CREATE TABLE lystore.title (
  id bigserial NOT NULL,
  name character varying(255),
  CONSTRAINT title_pkey PRIMARY KEY (id)
);
 CREATE TABLE lystore.grade (
  id bigserial NOT NULL,
  name character varying(255),
  CONSTRAINT grade_pkey PRIMARY KEY (id)
);
 CREATE TABLE lystore.project (
  id bigserial NOT NULL,
  name character varying (255) NOT NULL,
  description character varying(255),
  id_title bigint NOT NULL,
  id_grade bigint NOT NULL,
  building character varying(255),
  stair integer,
  room character varying(50),
  site character varying(255),
  CONSTRAINT project_pkey PRIMARY KEY (id),
  CONSTRAINT fk_title_id FOREIGN KEY (id_title)
    REFERENCES lystore.title (id) MATCH SIMPLE
    ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_grade_id FOREIGN KEY (id_grade)
    REFERENCES lystore.grade (id) MATCH SIMPLE
    ON UPDATE NO ACTION ON DELETE CASCADE
);

 ALTER TABLE lystore.order_client_equipment
ADD COLUMN id_project bigint;
 ALTER TABLE lystore.order_client_equipment
ADD CONSTRAINT fk_project_id  FOREIGN KEY (id_project)
REFERENCES lystore.project (id) MATCH SIMPLE
ON UPDATE NO ACTION ON DELETE CASCADE;

ALTER TABLE lystore.order_client_equipment
  DROP CONSTRAINT "status_values";

ALTER TABLE lystore.order_client_equipment
    ADD CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID', 'WAITING_FOR_ACCEPTANCE', 'REJECTED', 'SENT', 'DONE') );