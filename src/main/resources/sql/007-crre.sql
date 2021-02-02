DROP TABLE crre.licences;
CREATE TABLE crre.licences
(
    id_structure character varying(50) NOT NULL,
    amount numeric,
    initial_amount numeric,
    PRIMARY KEY (id_structure),
    FOREIGN KEY (id_structure)
        REFERENCES crre.students (id_structure) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)