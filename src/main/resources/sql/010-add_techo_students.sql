TRUNCATE TABLE crre.students;
ALTER TABLE crre.students
    ADD secondetechno bigint DEFAULT 0,
    ADD premieretechno bigint DEFAULT 0,
    ADD terminaletechno bigint DEFAULT 0;
