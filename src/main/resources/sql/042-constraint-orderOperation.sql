UPDATE lystore.order_client_equipment
SET status = 'IN PROGRESS'
WHERE id_operation is not null;

ALTER TABLE lystore.order_client_equipment
  ADD  CONSTRAINT valid_status_order CHECK ((id_operation is not null AND status = 'IN PROGRESS') OR (id_operation is null)) ;


