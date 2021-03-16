ALTER TABLE crre.quote
ALTER COLUMN creation_date type timestamp without time zone,
ALTER COLUMN creation_date set default now();

