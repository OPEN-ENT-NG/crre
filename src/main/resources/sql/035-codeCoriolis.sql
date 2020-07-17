ALTER TABLE Lystore.specific_structures
ADD COLUMN code_coriolis VARCHAR (250),
DROP      CONSTRAINT "specific_structure_type_values",
ADD     CONSTRAINT "specific_structure_type_values" CHECK (type IN ('CMD', 'CMR','LYC'))
 ;

