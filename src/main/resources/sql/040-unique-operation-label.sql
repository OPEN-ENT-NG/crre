ALTER  table  Lystore.operation
ADD CONSTRAINT operation_unicity_label_date UNIQUE (id_label,date_cp)
;