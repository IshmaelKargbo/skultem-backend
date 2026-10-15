ALTER TABLE public.id_card_settings
ADD COLUMN photo_size integer NOT NULL DEFAULT 100,
ADD COLUMN photo_padding integer NOT NULL DEFAULT 0;

ALTER TABLE public.id_card_settings
ADD COLUMN logo_box_size integer NOT NULL DEFAULT 100,
ADD COLUMN logo_padding integer NOT NULL DEFAULT 0;
