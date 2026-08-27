-- The `lessons` table was created in V1 as a bare shell (title, lesson, state) with
-- no domain code ever wired up to it (no repository implementation, no endpoints).
-- This extends it to hold an actual Sierra Leone lesson note: the daily record a
-- teacher writes against a week of the scheme of work, covering instructional
-- objectives, previous knowledge, teaching aids, reference materials, the
-- introduction/development/conclusion presentation, evaluation and assignment.
--
-- The table has never had a working write path, so it is empty in every
-- environment — safe to add a NOT NULL column without a default.

ALTER TABLE public.lessons
    ADD COLUMN date date NOT NULL,
    ADD COLUMN duration character varying(100),
    ADD COLUMN objectives jsonb,
    ADD COLUMN previous_knowledge text,
    ADD COLUMN teaching_aids jsonb,
    ADD COLUMN reference_materials jsonb,
    ADD COLUMN presentation jsonb,
    ADD COLUMN evaluation text,
    ADD COLUMN assignment text;

COMMENT ON COLUMN public.lessons.lesson IS 'Short content/focus summary of the lesson, e.g. "Introduction to simple and compound fractions"';
