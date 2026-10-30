-- Library Attendance (docs/LIBRARY_ATTENDANCE.md): two separate records of who was in the library.
--
--   library_visits            a student who came in on their own (check-in / check-out)
--   library_sessions          one occurrence of a Library period from the timetable, once opened
--   library_session_records   the session's roster and each student's mark
--   library_roster_changes    students added to / removed from a session's roster, with reasons
--   library_attendance_corrections   changes to a finalized session, with the original status
--
-- None of this touches the ordinary `attendance` table. Names and classes are copied onto the rows
-- (as library_loans does) because Skultem keeps no placement history: a class change overwrites the
-- enrollment, so the roster as it was on the day can only be kept by saving it.

-- The school's Library subject: timetable entries for this subject are library sessions.
ALTER TABLE public.library_settings
    ADD COLUMN library_subject_id character varying(255),
    ADD CONSTRAINT library_settings_library_subject_id_fkey
        FOREIGN KEY (library_subject_id) REFERENCES public.subjects(id) ON DELETE SET NULL;

CREATE TABLE public.library_visits (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    student_name character varying(255) NOT NULL,
    student_class character varying(255),
    status character varying(20) NOT NULL,
    checked_in_at timestamp(6) with time zone NOT NULL,
    checked_in_by character varying(255) NOT NULL,
    checked_in_by_name character varying(255),
    -- When the student's school day ends; an open visit after this needs following up.
    school_day_ends_at timestamp(6) with time zone NOT NULL,
    checked_out_at timestamp(6) with time zone,
    checked_out_by character varying(255),
    checked_out_by_name character varying(255),
    resolved_at timestamp(6) with time zone,
    resolved_by character varying(255),
    resolved_by_name character varying(255),
    resolution_reason character varying(500),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT library_visits_pkey PRIMARY KEY (id),
    CONSTRAINT library_visits_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id),
    CONSTRAINT library_visits_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id),
    CONSTRAINT library_visits_status_check CHECK (status IN ('OPEN', 'CHECKED_OUT', 'RESOLVED')),
    -- A closed visit says how it closed, and a resolution never carries a check-out time.
    CONSTRAINT library_visits_state_check CHECK (
        (status = 'OPEN' AND checked_out_at IS NULL AND resolved_at IS NULL)
        OR (status = 'CHECKED_OUT' AND checked_out_at IS NOT NULL AND checked_out_at >= checked_in_at AND resolved_at IS NULL)
        OR (status = 'RESOLVED' AND resolved_at IS NOT NULL AND checked_out_at IS NULL AND resolution_reason IS NOT NULL)
    )
);

-- One open visit per student per school - enforced here, so two check-ins at the same moment can't both land.
CREATE UNIQUE INDEX uq_library_visits_one_open ON public.library_visits (school_id, student_id) WHERE status = 'OPEN';
CREATE INDEX idx_library_visits_school_checked_in ON public.library_visits (school_id, checked_in_at);
CREATE INDEX idx_library_visits_student ON public.library_visits (student_id);

CREATE TABLE public.library_sessions (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    -- Null once the timetable entry is deleted; the session keeps its own copy of the details.
    timetable_entry_id character varying(255),
    session_date date NOT NULL,
    class_session_id character varying(255) NOT NULL,
    class_name character varying(255) NOT NULL,
    subject_id character varying(255),
    subject_name character varying(255) NOT NULL,
    period_name character varying(255),
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    room_name character varying(255),
    term_id character varying(255),
    -- Last day the session can still be marked (the end of its term).
    term_end_date date NOT NULL,
    status character varying(20) NOT NULL,
    opened_at timestamp(6) with time zone,
    opened_by character varying(255),
    opened_by_name character varying(255),
    finalized_at timestamp(6) with time zone,
    finalized_by character varying(255),
    finalized_by_name character varying(255),
    cancelled_at timestamp(6) with time zone,
    cancelled_by character varying(255),
    cancelled_by_name character varying(255),
    cancel_reason character varying(500),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT library_sessions_pkey PRIMARY KEY (id),
    CONSTRAINT library_sessions_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id),
    CONSTRAINT library_sessions_timetable_entry_fkey FOREIGN KEY (timetable_entry_id)
        REFERENCES public.timetables(id) ON DELETE SET NULL,
    CONSTRAINT library_sessions_class_session_fkey FOREIGN KEY (class_session_id) REFERENCES public.class_sessions(id),
    CONSTRAINT library_sessions_status_check CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT library_sessions_state_check CHECK (
        (status = 'IN_PROGRESS' AND finalized_at IS NULL AND cancelled_at IS NULL)
        OR (status = 'COMPLETED' AND finalized_at IS NOT NULL AND cancelled_at IS NULL)
        OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND finalized_at IS NULL AND cancel_reason IS NOT NULL)
    )
);

-- A timetable entry has at most one session per date, so two people opening it at once get one session.
CREATE UNIQUE INDEX uq_library_sessions_entry_date ON public.library_sessions (school_id, timetable_entry_id, session_date)
    WHERE timetable_entry_id IS NOT NULL;
CREATE INDEX idx_library_sessions_school_date ON public.library_sessions (school_id, session_date);
CREATE INDEX idx_library_sessions_class_session ON public.library_sessions (class_session_id);

CREATE TABLE public.library_session_records (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    session_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    student_name character varying(255) NOT NULL,
    student_class character varying(255),
    status character varying(20) NOT NULL,
    excuse_reason character varying(500),
    -- Taken off the roster before finalizing (kept, not deleted, and not counted).
    removed boolean NOT NULL DEFAULT false,
    added_manually boolean NOT NULL DEFAULT false,
    corrected boolean NOT NULL DEFAULT false,
    marked_at timestamp(6) with time zone,
    marked_by character varying(255),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT library_session_records_pkey PRIMARY KEY (id),
    CONSTRAINT library_session_records_session_fkey FOREIGN KEY (session_id) REFERENCES public.library_sessions(id),
    CONSTRAINT library_session_records_student_fkey FOREIGN KEY (student_id) REFERENCES public.students(id),
    CONSTRAINT library_session_records_status_check CHECK (status IN ('UNMARKED', 'PRESENT', 'ABSENT', 'EXCUSED')),
    CONSTRAINT uq_library_session_records_student UNIQUE (session_id, student_id)
);

CREATE INDEX idx_library_session_records_student ON public.library_session_records (school_id, student_id);

CREATE TABLE public.library_roster_changes (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    session_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    student_name character varying(255) NOT NULL,
    change character varying(20) NOT NULL,
    reason character varying(500) NOT NULL,
    changed_at timestamp(6) with time zone NOT NULL,
    changed_by character varying(255) NOT NULL,
    changed_by_name character varying(255),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT library_roster_changes_pkey PRIMARY KEY (id),
    CONSTRAINT library_roster_changes_session_fkey FOREIGN KEY (session_id) REFERENCES public.library_sessions(id),
    CONSTRAINT library_roster_changes_change_check CHECK (change IN ('ADDED', 'REMOVED'))
);

CREATE INDEX idx_library_roster_changes_session ON public.library_roster_changes (session_id);

CREATE TABLE public.library_attendance_corrections (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    session_id character varying(255) NOT NULL,
    record_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    student_name character varying(255) NOT NULL,
    original_status character varying(20) NOT NULL,
    corrected_status character varying(20) NOT NULL,
    reason character varying(500) NOT NULL,
    corrected_at timestamp(6) with time zone NOT NULL,
    corrected_by character varying(255) NOT NULL,
    corrected_by_name character varying(255),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT library_attendance_corrections_pkey PRIMARY KEY (id),
    CONSTRAINT library_attendance_corrections_session_fkey FOREIGN KEY (session_id) REFERENCES public.library_sessions(id),
    CONSTRAINT library_attendance_corrections_record_fkey FOREIGN KEY (record_id) REFERENCES public.library_session_records(id),
    CONSTRAINT library_attendance_corrections_status_check CHECK (
        original_status IN ('PRESENT', 'ABSENT', 'EXCUSED') AND corrected_status IN ('PRESENT', 'ABSENT', 'EXCUSED')
        AND original_status <> corrected_status
    )
);

CREATE INDEX idx_library_attendance_corrections_session ON public.library_attendance_corrections (session_id);
