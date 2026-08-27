-- A generated report card is a snapshot of one student's term result at the
-- moment "Generate" was run (grades, attendance %, rank, overall grade) -
-- re-generating the same student/term overwrites this row rather than
-- inserting a second one, hence the unique (school_id, student_id, term_id).

CREATE TABLE public.report_cards (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    student_name character varying(255) NOT NULL,
    admission_number character varying(255),
    photo character varying(255),
    class_id character varying(255) NOT NULL,
    class_name character varying(255),
    class_size integer NOT NULL DEFAULT 0,
    term_id character varying(255) NOT NULL,
    term_name character varying(255),
    academic_year_name character varying(255),
    average double precision NOT NULL DEFAULT 0,
    "position" integer NOT NULL DEFAULT 0,
    overall_grade character varying(255),
    passed boolean NOT NULL DEFAULT false,
    attendance_percentage double precision,
    remark text,
    subjects text NOT NULL,
    generated_by character varying(255),
    generated_at timestamp(6) with time zone,
    download_count integer NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT report_cards_pkey PRIMARY KEY (id),
    CONSTRAINT report_cards_school_student_term_key UNIQUE (school_id, student_id, term_id)
);

CREATE INDEX idx_report_cards_school_id ON public.report_cards (school_id);
CREATE INDEX idx_report_cards_class_id ON public.report_cards (class_id);
CREATE INDEX idx_report_cards_term_id ON public.report_cards (term_id);

ALTER TABLE ONLY public.report_cards
    ADD CONSTRAINT report_cards_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);

ALTER TABLE ONLY public.report_cards
    ADD CONSTRAINT report_cards_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id);

ALTER TABLE ONLY public.report_cards
    ADD CONSTRAINT report_cards_class_id_fkey FOREIGN KEY (class_id) REFERENCES public.classes(id);

ALTER TABLE ONLY public.report_cards
    ADD CONSTRAINT report_cards_term_id_fkey FOREIGN KEY (term_id) REFERENCES public.terms(id);
