CREATE TABLE crre.basket_order (
    id integer NOT NULL,
    name character varying(255),
    id_structure character varying(255),
    id_campaign bigint,
    name_user character varying(255),
    id_user character varying(255),
    total double precision,
    amount bigint,
    created date
);

ALTER TABLE crre.basket_order OWNER TO "web-education";

CREATE SEQUENCE crre.basket_order_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE crre.basket_order_id_seq OWNER TO "web-education";
ALTER SEQUENCE crre.basket_order_id_seq OWNED BY crre.basket_order.id;
ALTER TABLE ONLY crre.basket_order ALTER COLUMN id SET DEFAULT nextval('crre.basket_order_id_seq'::regclass);
ALTER TABLE ONLY crre.basket_order
    ADD CONSTRAINT basket_order_pkey PRIMARY KEY (id);
ALTER TABLE crre.order_client_equipment
ADD COLUMN id_basket bigint;
CREATE INDEX fki_fk_basket_id ON crre.order_client_equipment USING btree (id_basket);
ALTER TABLE ONLY crre.order_client_equipment
ADD CONSTRAINT fk_basket_order FOREIGN KEY (id_basket) REFERENCES crre.basket_order(id);