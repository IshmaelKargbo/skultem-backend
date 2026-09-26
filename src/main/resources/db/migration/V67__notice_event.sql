-- A notice can announce something that happens on a date (a PTA meeting, a sports day): when it occurs,
-- where, and optionally the calendar event that mirrors it. All optional - existing notices are untouched.
ALTER TABLE public.notices
    ADD COLUMN event_at timestamp(6) with time zone,
    ADD COLUMN event_ends_at timestamp(6) with time zone,
    ADD COLUMN event_location character varying(255),
    ADD COLUMN calendar_event_id character varying(255);

-- A notice about an event gets its own category.
ALTER TABLE public.notices DROP CONSTRAINT IF EXISTS notices_category_check;
ALTER TABLE public.notices ADD CONSTRAINT notices_category_check CHECK (((category)::text = ANY ((ARRAY[
    'GENERAL'::character varying, 'ACADEMIC'::character varying, 'FEE'::character varying,
    'URGENT'::character varying, 'EVENT'::character varying])::text[])));

CREATE INDEX IF NOT EXISTS idx_notices_event_at ON public.notices (school_id, event_at);
