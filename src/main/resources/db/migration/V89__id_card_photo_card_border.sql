ALTER TABLE public.id_card_settings
ADD COLUMN photo_border integer NOT NULL DEFAULT 0,
ADD COLUMN card_border integer NOT NULL DEFAULT 0;
