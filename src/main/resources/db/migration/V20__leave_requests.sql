-- Leave Management: replaces the previously mock-only /hr/leave frontend. Built on the existing
-- Teacher entity, same as payroll (V19) - this school system has no separate employee concept yet.
CREATE TABLE public.leave_requests (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    teacher_id character varying(255) NOT NULL,
    type character varying(50) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    reason character varying(1000) NOT NULL,
    status character varying(50) NOT NULL DEFAULT 'PENDING',
    review_note character varying(1000),
    reviewed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT leave_requests_pkey PRIMARY KEY (id),
    CONSTRAINT leave_requests_type_check CHECK (((type)::text = ANY ((ARRAY['ANNUAL'::character varying, 'SICK'::character varying, 'EMERGENCY'::character varying, 'MATERNITY'::character varying, 'PATERNITY'::character varying, 'STUDY'::character varying, 'UNPAID'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT leave_requests_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);

ALTER TABLE ONLY public.leave_requests
    ADD CONSTRAINT leave_requests_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);
ALTER TABLE ONLY public.leave_requests
    ADD CONSTRAINT leave_requests_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES public.teachers(id);
CREATE INDEX idx_leave_requests_school_id ON public.leave_requests (school_id);
CREATE INDEX idx_leave_requests_teacher_id ON public.leave_requests (teacher_id);

-- Staff (teacher) attendance - a genuinely separate concept from student attendance (which is
-- keyed off Enrollment). One row per teacher per day.
CREATE TABLE public.teacher_attendances (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    teacher_id character varying(255) NOT NULL,
    date date NOT NULL,
    status character varying(50) NOT NULL,
    note character varying(500),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT teacher_attendances_pkey PRIMARY KEY (id),
    CONSTRAINT teacher_attendances_school_teacher_date_key UNIQUE (school_id, teacher_id, date),
    CONSTRAINT teacher_attendances_status_check CHECK (((status)::text = ANY ((ARRAY['PRESENT'::character varying, 'LATE'::character varying, 'ABSENT'::character varying, 'EXCUSED'::character varying])::text[])))
);

ALTER TABLE ONLY public.teacher_attendances
    ADD CONSTRAINT teacher_attendances_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);
ALTER TABLE ONLY public.teacher_attendances
    ADD CONSTRAINT teacher_attendances_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES public.teachers(id);
CREATE INDEX idx_teacher_attendances_school_date ON public.teacher_attendances (school_id, date);
