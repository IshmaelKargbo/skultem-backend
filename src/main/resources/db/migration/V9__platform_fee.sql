ALTER TABLE fee_structures
    ADD COLUMN system boolean NOT NULL DEFAULT false;

CREATE TABLE platform_settings (
    id character varying(255) NOT NULL,
    platform_fee_amount numeric(38,2),
    updated_at timestamp(6) with time zone,
    CONSTRAINT platform_settings_pkey PRIMARY KEY (id)
);
