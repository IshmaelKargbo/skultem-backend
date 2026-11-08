-- Report card review: an admin generates a class's report cards, the class teacher checks them, adds comments and
-- approves them (or sends them back to the admin with a note). Parents only see approved cards. Cards generated
-- before this start approved, so nothing parents can already see disappears.
ALTER TABLE public.report_cards
    ADD COLUMN review_status character varying(20) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN review_note character varying(500),
    ADD COLUMN reviewed_by_name character varying(255),
    ADD COLUMN reviewed_at timestamp(6) with time zone,
    ADD CONSTRAINT report_cards_review_status_check
        CHECK (review_status IN ('PENDING_REVIEW', 'RETURNED', 'APPROVED'));

CREATE INDEX idx_report_cards_class_term_status ON public.report_cards (school_id, class_id, term_id, review_status);
