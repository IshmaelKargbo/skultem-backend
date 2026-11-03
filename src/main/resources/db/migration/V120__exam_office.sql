-- Exam Office.
--
-- EXAM_OFFICER: a staff member who runs the exam office - approves grades where the school routes them there,
-- tracks who has submitted, moves the assessment cycle on, generates report cards and keeps the exam timetable.
ALTER TABLE public.school_users DROP CONSTRAINT school_users_role_check;
ALTER TABLE public.school_users ADD CONSTRAINT school_users_role_check CHECK (
    role IN ('SYSTEM_ADMIN', 'OWNER', 'PROPRIETOR', 'PRINCIPAL', 'SUPER_ADMIN', 'ADMIN', 'ACCOUNTANT', 'LIBRARIAN',
             'NURSE', 'EXAM_OFFICER', 'TEACHER', 'PARENT')
);

-- GradeApprover.EXAM_OFFICER: a third choice, per school or per management section, for who approves grades.
ALTER TABLE public.schools DROP CONSTRAINT schools_grade_approver_check;
ALTER TABLE public.schools ADD CONSTRAINT schools_grade_approver_check
    CHECK (grade_approver IN ('CLASS_MASTER', 'ADMIN', 'EXAM_OFFICER'));
ALTER TABLE public.management_sections DROP CONSTRAINT management_sections_grade_approver_check;
ALTER TABLE public.management_sections ADD CONSTRAINT management_sections_grade_approver_check
    CHECK (grade_approver IS NULL OR grade_approver IN ('CLASS_MASTER', 'ADMIN', 'EXAM_OFFICER'));

-- The exam timetable: one row per paper a class sits (a subject can have more than one paper).
CREATE TABLE public.exam_schedules (
    id              character varying(255) PRIMARY KEY,
    school_id       character varying(255) NOT NULL,
    term_id         character varying(255) NOT NULL REFERENCES public.terms (id),
    class_id        character varying(255) NOT NULL REFERENCES public.classes (id),
    subject_id      character varying(255) NOT NULL REFERENCES public.subjects (id),
    paper           character varying(100),
    exam_date       date NOT NULL,
    start_time      time NOT NULL,
    end_time        time NOT NULL,
    venue           character varying(150),
    invigilator_id  character varying(255) REFERENCES public.teachers (id),
    notes           character varying(500),
    created_at      timestamp(6) with time zone,
    updated_at      timestamp(6) with time zone,
    CONSTRAINT exam_schedules_time_check CHECK (end_time > start_time)
);

CREATE INDEX idx_exam_schedules_school_term ON public.exam_schedules (school_id, term_id, exam_date);
CREATE INDEX idx_exam_schedules_invigilator ON public.exam_schedules (invigilator_id);
