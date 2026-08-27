-- Class-master-driven promotion workflow: a class master submits a promotion decision (promote/repeat
-- + remark) per student in their class session, an admin/proprietor reviews and approves it, and only
-- then does the enrollment migration ("the copy process") run. See PromotionRequest / PromotionRequestItem.

CREATE TABLE public.promotion_requests (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    class_session_id character varying(255) NOT NULL,
    class_master_id character varying(255) NOT NULL,
    academic_year_id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    teacher_note text,
    return_reason text,
    approval_note text,
    executed_at timestamp(6) with time zone,
    promoted_count integer NOT NULL DEFAULT 0,
    repeated_count integer NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT promotion_requests_pkey PRIMARY KEY (id),
    CONSTRAINT promotion_requests_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_REVIEW'::character varying, 'RETURNED'::character varying, 'APPROVED'::character varying])::text[]))),
    CONSTRAINT fk_promotion_requests_class_session FOREIGN KEY (class_session_id) REFERENCES public.class_sessions(id),
    CONSTRAINT fk_promotion_requests_class_master FOREIGN KEY (class_master_id) REFERENCES public.class_masters(id),
    CONSTRAINT fk_promotion_requests_academic_year FOREIGN KEY (academic_year_id) REFERENCES public.academic_years(id)
);

CREATE TABLE public.promotion_request_items (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    promotion_request_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    enrollment_id character varying(255) NOT NULL,
    outcome character varying(255) NOT NULL,
    remark text,
    CONSTRAINT promotion_request_items_pkey PRIMARY KEY (id),
    CONSTRAINT promotion_request_items_outcome_check CHECK (((outcome)::text = ANY ((ARRAY['PROMOTE'::character varying, 'REPEAT'::character varying])::text[]))),
    CONSTRAINT fk_promotion_request_items_request FOREIGN KEY (promotion_request_id) REFERENCES public.promotion_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_promotion_request_items_student FOREIGN KEY (student_id) REFERENCES public.students(id),
    CONSTRAINT fk_promotion_request_items_enrollment FOREIGN KEY (enrollment_id) REFERENCES public.enrollments(id)
);

-- Postgres never indexes FK columns automatically (see V3), and school_id is the near-universal
-- multi-tenant filter, so both need explicit indexes same as every other table.
CREATE INDEX idx_promotion_requests_school_id ON public.promotion_requests (school_id);
CREATE INDEX idx_promotion_requests_class_session_id ON public.promotion_requests (class_session_id);
CREATE INDEX idx_promotion_requests_class_master_id ON public.promotion_requests (class_master_id);
CREATE INDEX idx_promotion_requests_academic_year_id ON public.promotion_requests (academic_year_id);
CREATE INDEX idx_promotion_requests_status ON public.promotion_requests (status);

CREATE INDEX idx_promotion_request_items_school_id ON public.promotion_request_items (school_id);
CREATE INDEX idx_promotion_request_items_request_id ON public.promotion_request_items (promotion_request_id);
CREATE INDEX idx_promotion_request_items_student_id ON public.promotion_request_items (student_id);
CREATE INDEX idx_promotion_request_items_enrollment_id ON public.promotion_request_items (enrollment_id);
