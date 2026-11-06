-- Exam timetables: one per assessment per term (e.g. Term 1 "1st Test", Term 1 "Exam"). Each has an exam period,
-- instructions for candidates and a status - a DRAFT is seen only by the exam office, a PUBLISHED one by teachers
-- and parents too. A paper can be sat by several classes at once (all of JSS 1 in the hall) and watched by several
-- invigilators.
CREATE TABLE public.exam_timetables (
    id                character varying(255) PRIMARY KEY,
    school_id         character varying(255) NOT NULL REFERENCES public.schools (id),
    term_id           character varying(255) NOT NULL REFERENCES public.terms (id),
    assessment_id     character varying(255) NOT NULL REFERENCES public.assessments (id) ON DELETE CASCADE,
    title             character varying(150) NOT NULL,
    start_date        date,
    end_date          date,
    instructions      character varying(2000),
    status            character varying(16) NOT NULL DEFAULT 'DRAFT',
    published_at      timestamp(6) with time zone,
    published_by_name character varying(255),
    created_at        timestamp(6) with time zone,
    updated_at        timestamp(6) with time zone,
    CONSTRAINT exam_timetables_status_check CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT exam_timetables_period_check CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date),
    CONSTRAINT uq_exam_timetables_term_assessment UNIQUE (term_id, assessment_id)
);

CREATE INDEX idx_exam_timetables_school_term ON public.exam_timetables (school_id, term_id);

CREATE TABLE public.exam_papers (
    id            character varying(255) PRIMARY KEY,
    school_id     character varying(255) NOT NULL,
    timetable_id  character varying(255) NOT NULL REFERENCES public.exam_timetables (id) ON DELETE CASCADE,
    subject_id    character varying(255) NOT NULL REFERENCES public.subjects (id),
    paper         character varying(100),
    exam_date     date NOT NULL,
    start_time    time NOT NULL,
    end_time      time NOT NULL,
    venue         character varying(150),
    notes         character varying(500),
    created_at    timestamp(6) with time zone,
    updated_at    timestamp(6) with time zone,
    CONSTRAINT exam_papers_time_check CHECK (end_time > start_time)
);

CREATE INDEX idx_exam_papers_timetable ON public.exam_papers (timetable_id, exam_date);
CREATE INDEX idx_exam_papers_school_date ON public.exam_papers (school_id, exam_date);

CREATE TABLE public.exam_paper_classes (
    paper_id character varying(255) NOT NULL REFERENCES public.exam_papers (id) ON DELETE CASCADE,
    class_id character varying(255) NOT NULL REFERENCES public.classes (id) ON DELETE CASCADE,
    PRIMARY KEY (paper_id, class_id)
);

CREATE INDEX idx_exam_paper_classes_class ON public.exam_paper_classes (class_id);

CREATE TABLE public.exam_paper_invigilators (
    paper_id   character varying(255) NOT NULL REFERENCES public.exam_papers (id) ON DELETE CASCADE,
    teacher_id character varying(255) NOT NULL REFERENCES public.teachers (id) ON DELETE CASCADE,
    PRIMARY KEY (paper_id, teacher_id)
);

CREATE INDEX idx_exam_paper_invigilators_teacher ON public.exam_paper_invigilators (teacher_id);

-- Move the old term-wide papers in. Each goes to the timetable of its class's last assessment (usually the Exam),
-- published, since teachers could already see them.
CREATE TEMP TABLE exam_schedule_move AS
SELECT e.*,
       (SELECT a.id FROM public.assessments a
         JOIN public.classes c ON c.assessment_template_id = a.template_id
        WHERE c.id = e.class_id
        ORDER BY a.position DESC LIMIT 1) AS assessment_id
FROM public.exam_schedules e;

INSERT INTO public.exam_timetables (id, school_id, term_id, assessment_id, title, start_date, end_date, status,
                                    published_at, created_at, updated_at)
SELECT gen_random_uuid()::text, m.school_id, m.term_id, m.assessment_id, a.name, min(m.exam_date),
       max(m.exam_date), 'PUBLISHED', now(), now(), now()
FROM exam_schedule_move m
JOIN public.assessments a ON a.id = m.assessment_id
GROUP BY m.school_id, m.term_id, m.assessment_id, a.name;

INSERT INTO public.exam_papers (id, school_id, timetable_id, subject_id, paper, exam_date, start_time, end_time,
                                venue, notes, created_at, updated_at)
SELECT m.id, m.school_id, t.id, m.subject_id, m.paper, m.exam_date, m.start_time, m.end_time, m.venue, m.notes,
       m.created_at, m.updated_at
FROM exam_schedule_move m
JOIN public.exam_timetables t ON t.term_id = m.term_id AND t.assessment_id = m.assessment_id;

INSERT INTO public.exam_paper_classes (paper_id, class_id)
SELECT m.id, m.class_id FROM exam_schedule_move m
WHERE EXISTS (SELECT 1 FROM public.exam_papers p WHERE p.id = m.id);

INSERT INTO public.exam_paper_invigilators (paper_id, teacher_id)
SELECT m.id, m.invigilator_id FROM exam_schedule_move m
WHERE m.invigilator_id IS NOT NULL AND EXISTS (SELECT 1 FROM public.exam_papers p WHERE p.id = m.id);

DROP TABLE exam_schedule_move;
DROP TABLE public.exam_schedules;
