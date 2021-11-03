ALTER TABLE crre.structure
    ADD COLUMN city character varying;

ALTER TABLE crre.structure
    ADD COLUMN region character varying;

ALTER TABLE crre.purse
ALTER COLUMN amount TYPE double precision;

ALTER TABLE crre.purse
ALTER COLUMN initial_amount TYPE double precision;