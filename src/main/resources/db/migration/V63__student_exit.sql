-- A student who leaves the school without finishing: WITHDRAWN (family stops enrollment) or EXPELLED
-- (school removes them). Both drop out of rosters/attendance/grading (their enrollment becomes LEFT)
-- but stay on file - they can still owe fees - and can be reinstated. The reason/date/note explain why.
ALTER TABLE public.students DROP CONSTRAINT students_status_check;
ALTER TABLE public.students ADD CONSTRAINT students_status_check CHECK (
    (status)::text = ANY (ARRAY['ACTIVE', 'GRADUATED', 'TRANSFERRED', 'SUSPENDED', 'DELETED', 'WITHDRAWN', 'EXPELLED']::text[]));

ALTER TABLE public.students
    ADD COLUMN exit_reason character varying(100),
    ADD COLUMN exit_date date,
    ADD COLUMN exit_note text;
