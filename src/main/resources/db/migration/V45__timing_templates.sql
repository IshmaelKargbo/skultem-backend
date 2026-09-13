-- Timing becomes a reusable, named template instead of one singleton row per school, so a
-- school can run Primary on different hours from JSS/SSS (or share one template across levels).
-- Existing rows become each school's "Default" template - the fallback used by any Level that
-- doesn't have its own template assigned.
ALTER TABLE public.timings
    ADD COLUMN name character varying(255) NOT NULL DEFAULT 'Default',
    ADD COLUMN is_default boolean NOT NULL DEFAULT false;

UPDATE public.timings SET is_default = true;

-- At most one default template per school.
CREATE UNIQUE INDEX timings_school_default_idx ON public.timings (school_id) WHERE is_default;

-- Which Timing template applies to a given Level (PRIMARY/JSS/SSS) for a school. A level with no
-- row here falls back to that school's default template. timing_id is intentionally not unique -
-- the same template can be shared across more than one level (e.g. one "Secondary" template
-- covering both JSS and SSS).
CREATE TABLE public.timing_levels (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    timing_id character varying(255) NOT NULL,
    level character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT timing_levels_pkey PRIMARY KEY (id),
    CONSTRAINT timing_levels_level_check CHECK (((level)::text = ANY ((ARRAY['PRIMARY'::character varying, 'JSS'::character varying, 'SSS'::character varying])::text[]))),
    CONSTRAINT fk_timing_levels_timing FOREIGN KEY (timing_id) REFERENCES public.timings(id)
);

CREATE UNIQUE INDEX timing_levels_school_level_idx ON public.timing_levels (school_id, level);
CREATE INDEX timing_levels_timing_id_idx ON public.timing_levels (timing_id);
