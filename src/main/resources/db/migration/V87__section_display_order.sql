-- Class sections (A, B, C) get a rank so lists show JSS 1 A, B, C instead of whatever order they were
-- created in. Existing sections keep their creation order per school.
ALTER TABLE public.sections ADD COLUMN display_order integer NOT NULL DEFAULT 0;

UPDATE public.sections s
SET display_order = r.rn
FROM (
    SELECT id, row_number() OVER (PARTITION BY school_id ORDER BY created_at, name) AS rn
    FROM public.sections
) r
WHERE s.id = r.id;
