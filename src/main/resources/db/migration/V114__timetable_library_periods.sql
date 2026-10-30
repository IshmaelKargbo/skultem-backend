-- Library periods are set on the timetable itself: a slot holds either a subject lesson (SUBJECT, taught by the
-- teacher assigned to that subject) or a Library period (LIBRARY, run by the librarian - no subject, no teacher).
-- Library Attendance's scheduled sessions are the timetable's LIBRARY entries (docs/LIBRARY_ATTENDANCE.md).
ALTER TABLE public.timetables
    ADD COLUMN kind character varying(20) NOT NULL DEFAULT 'SUBJECT';

ALTER TABLE public.timetables
    ALTER COLUMN teacher_subject_id DROP NOT NULL;

ALTER TABLE public.timetables
    ADD CONSTRAINT timetables_kind_check CHECK (
        (kind = 'SUBJECT' AND teacher_subject_id IS NOT NULL)
        OR (kind = 'LIBRARY' AND teacher_subject_id IS NULL)
    );

-- A Library period takes the whole slot for the class, so there is at most one in a period/day.
CREATE UNIQUE INDEX uq_timetables_library_slot ON public.timetables (period_id, working_day_id)
    WHERE kind = 'LIBRARY';

CREATE INDEX idx_timetables_school_kind ON public.timetables (school_id, kind);

-- V113 picked a "Library subject" in Library Settings to find library periods. The timetable now says so
-- directly, so that setting goes.
ALTER TABLE public.library_settings
    DROP CONSTRAINT library_settings_library_subject_id_fkey,
    DROP COLUMN library_subject_id;
