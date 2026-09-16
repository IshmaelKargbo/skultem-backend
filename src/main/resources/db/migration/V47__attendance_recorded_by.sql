ALTER TABLE public.attendance
    ADD COLUMN recorded_by_user_id character varying(255);

CREATE INDEX attendance_school_date_idx ON public.attendance (school_id, date);
CREATE INDEX attendance_enrollment_date_idx ON public.attendance (enrollment_id, date);
