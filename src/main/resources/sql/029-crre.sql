ALTER TABLE crre.licences DROP CONSTRAINT licences_id_structure_fkey;
ALTER TABLE crre.licences
    ADD CONSTRAINT unique_id_structure UNIQUE (id_structure);