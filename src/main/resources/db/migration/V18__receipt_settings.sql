-- One receipt design per school, same "single active config" pattern as report_card_settings
-- and id_card_settings: an admin tunes accent color / logo / footer note / which optional
-- sections print, applied to every payment receipt the school generates.
CREATE TABLE public.receipt_settings (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    accent_color character varying(255) NOT NULL,
    logo_url character varying(255),
    footer_note character varying(255),
    show_watermark boolean NOT NULL DEFAULT true,
    show_amount_in_words boolean NOT NULL DEFAULT true,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT receipt_settings_pkey PRIMARY KEY (id),
    CONSTRAINT receipt_settings_school_id_key UNIQUE (school_id)
);

ALTER TABLE ONLY public.receipt_settings
    ADD CONSTRAINT receipt_settings_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);

-- Re-reading every payment that shares a receipt number (the "View"/"Download" actions on a
-- past payment need to rebuild the whole original receipt, not just the one line item clicked)
-- means looking payments up by (school_id, reference_no) - give that its own index.
CREATE INDEX IF NOT EXISTS idx_payments_school_reference_no ON public.payments (school_id, reference_no);
