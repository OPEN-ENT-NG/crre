CREATE TABLE crre.structure_student
(
    id_structure character varying(50),
    total bigint DEFAULT 0,
    PRIMARY KEY (id_structure),
    CONSTRAINT structure UNIQUE (id_structure)
)

CREATE TABLE crre.students
(
    "id_structure" character varying(50),
	"6eme" bigint DEFAULT 0,
	"5eme" bigint DEFAULT 0,
	"4eme" bigint DEFAULT 0,
	"3eme" bigint DEFAULT 0,
	"Seconde" bigint DEFAULT 0,
	"Premiere" bigint DEFAULT 0,
	"Terminale" bigint DEFAULT 0,
    "pro" boolean,
    PRIMARY KEY (id_structure),
    CONSTRAINT structure_unique UNIQUE (id_structure)
)
