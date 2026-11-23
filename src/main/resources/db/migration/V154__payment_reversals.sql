-- A payment recorded by mistake (the student never paid) is reversed: the payment row is removed so every balance and
-- report is right again, and this table keeps what it was, who reversed it and why.
CREATE TABLE payment_reversals (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    payment_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    fee_id character varying(255) NOT NULL,
    fee_name character varying(255),
    term_name character varying(255),
    amount numeric(38, 2) NOT NULL,
    method character varying(32),
    receipt_no character varying(255),
    external_reference character varying(255),
    paid_at timestamp(6) with time zone,
    recorded_by_user_id character varying(255),
    reason text NOT NULL,
    reversed_by_user_id character varying(255),
    reversed_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT payment_reversals_pkey PRIMARY KEY (id),
    CONSTRAINT payment_reversals_payment_key UNIQUE (payment_id)
);
CREATE INDEX idx_payment_reversals_school_student ON payment_reversals (school_id, student_id);
