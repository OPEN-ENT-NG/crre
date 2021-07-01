ALTER TABLE crre.campaign
ADD COLUMN start_date DATE ,
ADD COLUMN  end_date DATE,
ADD COLUMN  automatic_close BOOLEAN DEFAULT false,
ADD COLUMN reassort BOOLEAN,
ADD COLUMN catalog character varying,
ADD COLUMN use_credit boolean,
ADD COLUMN id_type bigint
ALTER COLUMN purse_enabled SET DEFAULT true;


CREATE OR REPLACE FUNCTION crre.update_old_end_date() RETURNS TRIGGER AS $$
BEGIN
         UPDATE crre.campaign
         SET end_date = NULL
		 WHERE end_date <  NOW() AND automatic_close = true AND accessible = true;
		 RETURN NEW ;
END;
$$ LANGUAGE 'plpgsql';


CREATE TRIGGER update_old_end_date_trigger AFTER
UPDATE ON crre.campaign
FOR EACH ROW EXECUTE PROCEDURE crre.update_old_end_date();

CREATE TABLE crre.type_campaign (
    id bigint,
    name character varying,
    credit boolean,
    reassort boolean,
    catalog character varying,
    automatic_close boolean,
    structure character varying
);

INSERT INTO crre.type_campaign (id, name, credit, reassort, catalog, automatic_close, structure) VALUES (1, 'Test Type 1', true, false, 'Catalogue papier', true, 'cataloguepapier');
INSERT INTO crre.type_campaign (id, name, credit, reassort, catalog, automatic_close, structure) VALUES (2, 'Test Type 2', false, false, 'Catalogue numérique', false, 'cataloguenumérique');