ALTER TABLE lystore.project ALTER COLUMN id_grade DROP NOT NULL;
INSERT INTO lystore.title (name) VALUES ('Campagne PPE');

CREATE OR REPLACE FUNCTION project_data_recovery()
RETURNS VOID as $$
DECLARE
  structures record;
  structure_id character varying(36);
BEGIN
  FOR structures IN (
    SELECT distinct(id_structure) as id
    FROM lystore.order_client_equipment
  )
  LOOP
    structure_id := structures.id;
    WITH project AS (
        INSERT INTO lystore.project (id_title)
        VALUES (
          (SELECT id FROM lystore.title WHERE name = 'Campagne PPE' LIMIT 1)
        )
        RETURNING id
    )
    UPDATE lystore.order_client_equipment
    SET id_project = project.id
    FROM project
    WHERE id_structure = structure_id;
  END LOOP;
END;
$$ LANGUAGE plpgsql;

SELECT project_data_recovery();

DROP FUNCTION project_data_recovery();