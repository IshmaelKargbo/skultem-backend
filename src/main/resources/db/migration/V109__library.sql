-- Library module: book categories, the book catalog, and loans (a copy of a book issued to a
-- student or teacher). A book tracks how many copies the school owns and how many are on the
-- shelf right now; issuing a loan takes one copy off the shelf and returning it puts it back.
--
-- A loan is stored as ISSUED or RETURNED only. "Overdue" is not stored - it is an ISSUED loan whose
-- due date has passed, worked out when the loan is read, so nothing has to flip it on a schedule.
-- The borrower's name and class are copied onto the loan when it is issued, so the history still
-- reads correctly after a student changes class or leaves.

CREATE TABLE public.library_categories (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(500) NOT NULL DEFAULT '',
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT library_categories_pkey PRIMARY KEY (id),
    CONSTRAINT library_categories_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id)
);

CREATE INDEX idx_library_categories_school_id ON public.library_categories (school_id);

CREATE TABLE public.library_books (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    category_id character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    author character varying(255) NOT NULL,
    isbn character varying(32),
    publisher character varying(255),
    shelf character varying(50),
    total_copies integer NOT NULL,
    available_copies integer NOT NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT library_books_pkey PRIMARY KEY (id),
    CONSTRAINT library_books_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id),
    CONSTRAINT library_books_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.library_categories(id),
    CONSTRAINT library_books_copies_check CHECK (total_copies >= 0 AND available_copies >= 0 AND available_copies <= total_copies)
);

CREATE INDEX idx_library_books_school_id ON public.library_books (school_id);
CREATE INDEX idx_library_books_category_id ON public.library_books (category_id);

CREATE TABLE public.library_loans (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    book_id character varying(255) NOT NULL,
    borrower_type character varying(20) NOT NULL,
    student_id character varying(255),
    teacher_id character varying(255),
    borrower_name character varying(255) NOT NULL,
    borrower_class character varying(255),
    issue_date date NOT NULL,
    due_date date NOT NULL,
    returned_at timestamp(6) with time zone,
    status character varying(20) NOT NULL,
    notified_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT library_loans_pkey PRIMARY KEY (id),
    CONSTRAINT library_loans_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id),
    CONSTRAINT library_loans_book_id_fkey FOREIGN KEY (book_id) REFERENCES public.library_books(id),
    CONSTRAINT library_loans_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id),
    CONSTRAINT library_loans_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES public.teachers(id),
    CONSTRAINT library_loans_status_check CHECK (((status)::text = ANY ((ARRAY['ISSUED'::character varying, 'RETURNED'::character varying])::text[]))),
    CONSTRAINT library_loans_borrower_type_check CHECK (((borrower_type)::text = ANY ((ARRAY['STUDENT'::character varying, 'TEACHER'::character varying])::text[]))),
    CONSTRAINT library_loans_borrower_check CHECK (
        (borrower_type = 'STUDENT' AND student_id IS NOT NULL AND teacher_id IS NULL)
        OR (borrower_type = 'TEACHER' AND teacher_id IS NOT NULL AND student_id IS NULL)
    ),
    CONSTRAINT library_loans_dates_check CHECK (due_date >= issue_date)
);

CREATE INDEX idx_library_loans_school_id ON public.library_loans (school_id);
CREATE INDEX idx_library_loans_book_id ON public.library_loans (book_id);
CREATE INDEX idx_library_loans_status_due ON public.library_loans (school_id, status, due_date);
CREATE INDEX idx_library_loans_student_id ON public.library_loans (student_id);
CREATE INDEX idx_library_loans_teacher_id ON public.library_loans (teacher_id);

-- Library actions are logged with ActivityType.LIBRARY, which the activities type check has to allow
-- (see V41 for what happens when it doesn't: the whole action rolls back).
ALTER TABLE public.activities DROP CONSTRAINT activities_type_check;
ALTER TABLE public.activities ADD CONSTRAINT activities_type_check
    CHECK (((type)::text = ANY (ARRAY[('STUDENT'::character varying)::text, ('TEACHER'::character varying)::text, ('PARENT'::character varying)::text, ('USER'::character varying)::text, ('CLASS'::character varying)::text, ('SUBJECT'::character varying)::text, ('GRADE'::character varying)::text, ('FEES'::character varying)::text, ('EXPENSE'::character varying)::text, ('PAYMENT'::character varying)::text, ('SCHOOL'::character varying)::text, ('SETTINGS'::character varying)::text, ('SUPPLY'::character varying)::text, ('SALE'::character varying)::text, ('LIBRARY'::character varying)::text])));
