-- Per-school borrowing rules for the library. A school with no row uses the defaults in
-- LibrarySetting.defaults(): students hold up to 2 books, teachers up to 5, an overdue book blocks
-- new loans, and books are lent for 14 days unless the librarian picks another date.
CREATE TABLE public.library_settings (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    max_student_loans integer NOT NULL,
    max_teacher_loans integer NOT NULL,
    block_when_overdue boolean NOT NULL,
    default_loan_days integer NOT NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT library_settings_pkey PRIMARY KEY (id),
    CONSTRAINT uq_library_settings_school UNIQUE (school_id),
    CONSTRAINT library_settings_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id),
    CONSTRAINT library_settings_limits_check CHECK (
        max_student_loans BETWEEN 1 AND 50
        AND max_teacher_loans BETWEEN 1 AND 50
        AND default_loan_days BETWEEN 1 AND 365
    )
);
