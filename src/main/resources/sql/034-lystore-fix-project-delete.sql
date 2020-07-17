CREATE OR REPLACE FUNCTION delete_empty_project()
RETURNS trigger AS  $$
BEGIN
DELETE FROM Lystore.project as prj
	WHERE prj.id NOT IN (SELECT pp.id FROM Lystore.project as pp
 	  INNER JOIN Lystore.order_client_equipment oce
	  ON oce.id_project = pp.id
	GROUP BY pp.id)
	AND prj.id NOT IN (SELECT pp.id FROM Lystore.project as pp
 	  INNER JOIN Lystore."order-region-equipment" ore
	  ON ore.id_project = pp.id
	GROUP BY pp.id)
;
RETURN NULL;
END;
 $$  LANGUAGE plpgsql;
