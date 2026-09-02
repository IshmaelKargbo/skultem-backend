-- One payslip design per school, same "single active config" pattern as receipt_settings and
-- report_card_settings: an admin tunes accent color / logo / footer note / optional sections,
-- applied to every payslip the school generates.
CREATE TABLE public.payslip_settings (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    accent_color character varying(255) NOT NULL,
    logo_url character varying(255),
    footer_note character varying(255),
    show_watermark boolean NOT NULL DEFAULT true,
    show_amount_in_words boolean NOT NULL DEFAULT true,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT payslip_settings_pkey PRIMARY KEY (id),
    CONSTRAINT payslip_settings_school_id_key UNIQUE (school_id)
);

ALTER TABLE ONLY public.payslip_settings
    ADD CONSTRAINT payslip_settings_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);
