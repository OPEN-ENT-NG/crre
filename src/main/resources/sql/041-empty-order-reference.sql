
CREATE OR REPLACE FUNCTION order_without_ref()
RETURNS trigger AS  $$
BEGIN
DELETE FROM Lystore.order
WHERE id NOT IN (
    SELECT DISTINCT id_order FROM Lystore.order_client_equipment
     WHERE id_order IS NOT NULL
     ORDER BY id_order );
    RETURN NULL;
END;
 $$  LANGUAGE plpgsql;

CREATE TRIGGER check_order_no_ref AFTER UPDATE
ON Lystore.order_client_equipment
FOR EACH ROW
EXECUTE PROCEDURE order_without_ref();
