ALTER table lystore.operation
ADD date_cp date;

CREATE TABLE lystore.chapter
(
    id bigserial NOT NULL,
    code bigint NOT NULL,
    label character varying(200) NOT NULL,
    section character varying(50) NOT NULL,
    label_section character(100) NOT NULL,
    date_validity_start date NOT NULL,
    date_validity_end date NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_chapter_code UNIQUE (code)
);

CREATE TABLE lystore.functional_code
(
    id bigserial NOT NULL,
    code bigint NOT NULL,
    label character varying(200) NOT NULL,
    date_validity_start date,
    date_validity_end date,
    PRIMARY KEY (id),
    CONSTRAINT uq_functional_code UNIQUE (code)
);


INSERT INTO lystore.chapter(code, label, section, label_section, date_validity_start, date_validity_end )
 VALUES (902,'Enseignement','I','INVESTISSEMENT','2019-01-01','9999-12-31'),
        (932,'Enseignement','F','FONCTIONNEMENT','2019-01-01','9999-12-31');

INSERT INTO lystore.functional_code(code, label, date_validity_start, date_validity_end )
 VALUES (222,'Lycées publics','2019-01-01','9999-12-31'),
        (224,'Participation à des cités mixtes','2019-01-01','9999-12-31'),
        (28,'Autres services périscolaires et annexes','2019-01-01','9999-12-31');

ALTER TABLE lystore.program
    ADD CONSTRAINT fk_functional_code FOREIGN KEY (functional_code)
    REFERENCES lystore.functional_code (code) MATCH SIMPLE;

ALTER TABLE lystore.program
    ADD CONSTRAINT fk_chapter FOREIGN KEY (chapter)
    REFERENCES lystore.chapter (code) MATCH SIMPLE;

