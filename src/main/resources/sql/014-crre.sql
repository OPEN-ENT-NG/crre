CREATE TABLE crre.quote
(
    id bigserial NOT NULL,
    title character varying,
    creation_date date NOT NULL DEFAULT CURRENT_DATE,
    owner_name character varying,
    owner_id character varying,
    nb_structures integer,
    attachment character varying,
    quotation character varying,
    PRIMARY KEY (id)
)