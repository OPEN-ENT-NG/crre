CREATE INDEX equipment_idcontract_name_reference_idx ON lystore.equipment USING btree (id_contract, reference, name);
