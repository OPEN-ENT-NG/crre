ALTER TABLE lystore.contract DROP COLUMN price_editable;

ALTER TABLE lystore.equipment ADD COLUMN price_editable boolean NOT NULL DEFAULT false;