ALTER TABLE lystore.contract
ADD COLUMN file boolean NOT NULL DEFAULT false;

ALTER TABLE lystore.contract
ADD COLUMN price_editable boolean NOT NULL DEFAULT false;