-- One ID card design per school (Notice Board's Communicate pattern, not a
-- multi-template gallery) — see the id-cards rebuild scope decision.

CREATE TABLE public.id_card_settings (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    layout character varying(255) NOT NULL,
    profile_shape character varying(255) NOT NULL,
    header_color character varying(255) NOT NULL,
    footer_color character varying(255) NOT NULL,
    header_text_color character varying(255) NOT NULL,
    primary_text_color character varying(255) NOT NULL,
    width_mm integer NOT NULL,
    height_mm integer NOT NULL,
    bg_image_url character varying(255),
    bg_opacity integer NOT NULL,
    school_name character varying(255),
    school_address character varying(255),
    principal_name character varying(255),
    fields text NOT NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT id_card_settings_pkey PRIMARY KEY (id),
    CONSTRAINT id_card_settings_school_id_key UNIQUE (school_id)
);

ALTER TABLE ONLY public.id_card_settings
    ADD CONSTRAINT id_card_settings_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);
