CREATE TABLE lystore.exercise
(
    id bigserial,
    year character varying(50),
    PRIMARY KEY (id),
    CONSTRAINT uq_year UNIQUE (year)
);

CREATE TABLE lystore.instruction
(
    id bigserial,
    id_exercise bigint,
    object character varying(50),
	service_number  character varying(50),
    cp_number character varying(50),
    submitted_to_cp boolean,
    date_cp date,
    comment text,
    PRIMARY KEY (id),
    CONSTRAINT fk_id_exercise FOREIGN KEY (id_exercise)
        REFERENCES lystore.exercise (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

ALTER TABLE lystore.operation
    ADD CONSTRAINT fk_instruction_id FOREIGN KEY (id_instruction)
    REFERENCES lystore.instruction (id) MATCH SIMPLE;
