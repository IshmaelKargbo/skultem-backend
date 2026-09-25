-- Idempotency keys for endpoints that create records (enrolling a student, creating/assigning a fee,
-- recording a payment): a client that retries the same request - double click, flaky connection,
-- a retry after a timeout - sends the same Idempotency-Key and gets the first response back
-- instead of creating a duplicate. IN_PROGRESS rows guard a concurrent duplicate; COMPLETED rows
-- hold the response to replay. Rows are short-lived (see IdempotencyStore for the expiry).
CREATE TABLE public.idempotency_keys (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL,
    operation character varying(100) NOT NULL,
    idem_key character varying(200) NOT NULL,
    request_hash character varying(64) NOT NULL,
    status character varying(20) NOT NULL,
    response_body text,
    created_at timestamp(6) with time zone NOT NULL,
    completed_at timestamp(6) with time zone,
    CONSTRAINT idempotency_keys_pkey PRIMARY KEY (id),
    CONSTRAINT idempotency_keys_status_check CHECK (status IN ('IN_PROGRESS', 'COMPLETED'))
);

CREATE UNIQUE INDEX uq_idempotency_keys ON public.idempotency_keys (school_id, operation, idem_key);
CREATE INDEX idx_idempotency_keys_created_at ON public.idempotency_keys (created_at);
