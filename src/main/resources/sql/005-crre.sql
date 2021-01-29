CREATE TABLE crre.students
(
    "id_structure" character varying(50),
	"Seconde" bigint DEFAULT 0,
	"Premiere" bigint DEFAULT 0,
	"Terminale" bigint DEFAULT 0,
    "pro" boolean,
    PRIMARY KEY (id_structure),
    CONSTRAINT structure_unique UNIQUE (id_structure)
)
