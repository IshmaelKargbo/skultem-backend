-- Materials now carry a selling price, so a Sale can pull its unit price from the catalog instead
-- of someone re-typing it (and risking a typo) on every single sale - see MaterialSaleController.
-- Existing rows default to 0, an obviously-incomplete price a school will notice and fill in via
-- the Materials page, rather than guessing at a number for them.
ALTER TABLE public.materials
    ADD COLUMN price numeric(19,2) NOT NULL DEFAULT 0;
