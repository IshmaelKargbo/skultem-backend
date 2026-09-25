-- School structure: which educational levels a school offers, and how it chooses to manage them.
-- Three separate ideas that must not be mixed together:
--   * school_levels       - the levels a school offers (Daycare, Nursery, Primary, JSS, SSS, ...).
--   * management_sections - how the school groups those levels for management ("Early Years &
--                           Primary", "JSS", ...). Only used when management_model = SECTION_BASED.
--   * (later) user scope  - which management sections a staff member can access.
-- None of this is Section (a class division like "A") or Stream (Science/Arts) - those stay as-is.

-- 1. New levels. Every class already carries its level; Daycare and Nursery join the existing three.
ALTER TABLE public.classes DROP CONSTRAINT IF EXISTS classes_level_check;
ALTER TABLE public.classes ADD CONSTRAINT classes_level_check CHECK (((level)::text = ANY ((ARRAY[
    'DAYCARE'::character varying, 'NURSERY'::character varying, 'PRIMARY'::character varying,
    'JSS'::character varying, 'SSS'::character varying])::text[])));

ALTER TABLE public.timing_levels DROP CONSTRAINT IF EXISTS timing_levels_level_check;
ALTER TABLE public.timing_levels ADD CONSTRAINT timing_levels_level_check CHECK (((level)::text = ANY ((ARRAY[
    'DAYCARE'::character varying, 'NURSERY'::character varying, 'PRIMARY'::character varying,
    'JSS'::character varying, 'SSS'::character varying])::text[])));

-- 2. Management model. Every existing school is UNIFIED - one management scope across the whole
-- school, which is exactly how the system has behaved until now.
ALTER TABLE public.schools
    ADD COLUMN management_model character varying(20) NOT NULL DEFAULT 'UNIFIED',
    ADD CONSTRAINT schools_management_model_check
        CHECK (((management_model)::text = ANY ((ARRAY['UNIFIED'::character varying, 'SECTION_BASED'::character varying])::text[])));

-- 3. Management sections - school-defined, any number of them.
CREATE TABLE public.management_sections (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    display_order integer NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT management_sections_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_management_sections_school ON public.management_sections (school_id);

-- 4. Levels a school offers. One row per (school, level) - never duplicated per section. A level
-- belongs to at most one management section (management_section_id), so every level has one clear
-- management owner; it's null in UNIFIED mode, where no sections exist at all.
CREATE TABLE public.school_levels (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    level character varying(20) NOT NULL,
    management_section_id character varying(255),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT school_levels_pkey PRIMARY KEY (id),
    CONSTRAINT school_levels_level_check CHECK (((level)::text = ANY ((ARRAY[
        'DAYCARE'::character varying, 'NURSERY'::character varying, 'PRIMARY'::character varying,
        'JSS'::character varying, 'SSS'::character varying])::text[]))),
    CONSTRAINT fk_school_levels_management_section FOREIGN KEY (management_section_id)
        REFERENCES public.management_sections(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uq_school_levels_school_level ON public.school_levels (school_id, level);
CREATE INDEX idx_school_levels_management_section ON public.school_levels (management_section_id);

-- 5. Backfill. A school offers every level it already has (non-deleted) classes in - nothing about
-- its classes, students, fees or records changes. A school with no classes yet gets the three levels
-- the system assumed until now (Primary, JSS, SSS); it can change that in Settings.
INSERT INTO public.school_levels (id, school_id, level, created_at, updated_at)
SELECT gen_random_uuid()::text, c.school_id, c.level, now(), now()
FROM (SELECT DISTINCT school_id, level FROM public.classes WHERE status <> 'DELETED') c;

INSERT INTO public.school_levels (id, school_id, level, created_at, updated_at)
SELECT gen_random_uuid()::text, s.id, l.level, now(), now()
FROM public.schools s
CROSS JOIN (VALUES ('PRIMARY'), ('JSS'), ('SSS')) AS l(level)
WHERE NOT EXISTS (SELECT 1 FROM public.school_levels sl WHERE sl.school_id = s.id);
