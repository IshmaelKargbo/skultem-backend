ALTER TABLE public.id_card_settings
ADD COLUMN logo_radius integer NOT NULL DEFAULT 50,
ADD COLUMN signature_size integer NOT NULL DEFAULT 100;
