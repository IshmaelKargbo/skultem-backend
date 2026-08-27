-- One report card design per school, same "single active config" pattern as
-- id_card_settings - no multi-template gallery.

CREATE TABLE public.report_card_settings (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    header_color character varying(255) NOT NULL,
    logo_url character varying(255),
    footer_note character varying(255),
    show_attendance boolean NOT NULL DEFAULT true,
    show_remarks boolean NOT NULL DEFAULT true,
    show_position boolean NOT NULL DEFAULT true,
    show_signatures boolean NOT NULL DEFAULT true,
    show_grade_scale boolean NOT NULL DEFAULT true,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT report_card_settings_pkey PRIMARY KEY (id),
    CONSTRAINT report_card_settings_school_id_key UNIQUE (school_id)
);

ALTER TABLE ONLY public.report_card_settings
    ADD CONSTRAINT report_card_settings_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);
