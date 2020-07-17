CREATE OR REPLACE FUNCTION lystore.create_equipment_reference()
RETURNS trigger AS $BODY$
BEGIN
  IF NEW.reference IS NULL THEN
    UPDATE lystore.equipment SET reference = ('REF#' || NEW.id) WHERE id = NEW.id;
  END IF;
  RETURN NEW;
END;
$BODY$
LANGUAGE plpgsql VOLATILE;

CREATE TRIGGER on_equipment_insert
AFTER INSERT  ON lystore.equipment
FOR EACH ROW
EXECUTE PROCEDURE lystore.create_equipment_reference();

ALTER TABLE lystore.order_client_equipment
  ADD COLUMN "comment" TEXT ;

ALTER TABLE lystore.basket_equipment
  ADD COLUMN "comment" TEXT ;

UPDATE lystore.equipment SET reference = ('REF#' || id ) WHERE reference IS NULL;