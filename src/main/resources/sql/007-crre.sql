ALTER TABLE crre.purse
    ADD consumable_amount double precision CONSTRAINT "Check_consumable_amount_positive" CHECK (consumable_amount >= 0::numeric),
    ADD consumable_initial_amount double precision CONSTRAINT "Check_initial_consumable_amount_positive" CHECK (consumable_initial_amount >= 0::numeric),
    ADD CONSTRAINT "Check_initial_amount_positive" CHECK (initial_amount >= 0::numeric);

ALTER TABLE crre.purse ALTER consumable_amount SET DEFAULT 0;
ALTER TABLE crre.purse ALTER consumable_initial_amount SET DEFAULT 0;
ALTER TABLE crre.purse ALTER amount SET DEFAULT 0;
ALTER TABLE crre.purse ALTER initial_amount SET DEFAULT 0;