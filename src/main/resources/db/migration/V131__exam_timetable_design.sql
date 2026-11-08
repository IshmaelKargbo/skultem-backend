-- How a school's exam timetable PDFs look (Exam Office > Timetable > Design), one row per school - the same
-- "single active config" pattern as timetable_settings. No row = the defaults.
CREATE TABLE public.exam_timetable_settings (
    school_id          character varying(255) PRIMARY KEY REFERENCES public.schools (id),
    heading            character varying(120) NOT NULL DEFAULT 'EXAMINATION TIMETABLE',
    accent_color       character varying(20),
    orientation        character varying(20) NOT NULL DEFAULT 'PORTRAIT',
    show_logo          boolean NOT NULL DEFAULT true,
    show_venue         boolean NOT NULL DEFAULT true,
    show_duration      boolean NOT NULL DEFAULT true,
    show_invigilators  boolean NOT NULL DEFAULT false,
    show_instructions  boolean NOT NULL DEFAULT true,
    show_signatures    boolean NOT NULL DEFAULT true,
    left_signature     character varying(60) NOT NULL DEFAULT 'Exam Officer',
    right_signature    character varying(60) NOT NULL DEFAULT 'Principal',
    footer_note        character varying(255),
    updated_at         timestamp(6) with time zone,
    CONSTRAINT exam_timetable_settings_orientation_check CHECK (orientation IN ('PORTRAIT', 'LANDSCAPE'))
);
