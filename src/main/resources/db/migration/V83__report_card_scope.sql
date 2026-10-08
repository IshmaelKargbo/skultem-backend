-- A report card can now cover just some of a term's assessments (e.g. "First Test + Second Test"),
-- or every term of the academic year, so one student/term can have several cards. scope_key says
-- which: 'ALL' (every assessment of the term - the original behaviour), 'YEAR' (all terms), or the
-- chosen assessment ids, sorted and comma-joined. scope_label is the human-readable version shown
-- on the card; null for 'ALL'.
ALTER TABLE public.report_cards ADD COLUMN scope_key character varying(1000) NOT NULL DEFAULT 'ALL';
ALTER TABLE public.report_cards ADD COLUMN scope_label character varying(500);

ALTER TABLE public.report_cards DROP CONSTRAINT report_cards_school_student_term_key;
ALTER TABLE public.report_cards
    ADD CONSTRAINT report_cards_school_student_term_scope_key UNIQUE (school_id, student_id, term_id, scope_key);
