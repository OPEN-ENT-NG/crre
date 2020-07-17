 ALTER TABLE lystore.order_client_equipment
  DROP CONSTRAINT "status_values",
  ADD CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN PROGRESS', 'WAITING_FOR_ACCEPTANCE', 'REJECTED', 'SENT', 'DONE') );

ALTER TABLE lystore."order-region-equipment"
 DROP CONSTRAINT "status_values",
   ALTER id_structure TYPE CHARACTER varying(300),

 ADD CONSTRAINT "status_values" CHECK (status IN ('WAITING', 'VALID','IN PROGRESS', 'WAITING_FOR_ACCEPTANCE', 'REJECTED', 'SENT', 'DONE') );