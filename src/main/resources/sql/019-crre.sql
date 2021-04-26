ALTER TABLE crre.quote
    ALTER COLUMN creation_date TYPE timestamp with time zone ;

ALTER TABLE crre.scripts
    ALTER COLUMN passed TYPE timestamp with time zone ;

ALTER TABLE crre.logs
    ALTER COLUMN date TYPE timestamp with time zone ;