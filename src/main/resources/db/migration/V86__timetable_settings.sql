-- One timetable PDF design per school, same "single active config" pattern as payslip_settings and
-- receipt_settings: an admin tunes the title, colour, orientation and which details appear, applied to
-- every timetable PDF the school's admins, teachers and parents download.
CREATE TABLE public.timetable_settings (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    title character varying(255) NOT NULL DEFAULT 'SCHOOL TIMETABLE',
    accent_color character varying(255),
    orientation character varying(20) NOT NULL DEFAULT 'LANDSCAPE',
    show_logo boolean NOT NULL DEFAULT true,
    show_icons boolean NOT NULL DEFAULT true,
    show_teacher boolean NOT NULL DEFAULT true,
    show_room boolean NOT NULL DEFAULT true,
    show_period_times boolean NOT NULL DEFAULT true,
    footer_note character varying(255),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT timetable_settings_pkey PRIMARY KEY (id),
    CONSTRAINT timetable_settings_school_id_key UNIQUE (school_id)
);

ALTER TABLE ONLY public.timetable_settings
    ADD CONSTRAINT timetable_settings_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);
