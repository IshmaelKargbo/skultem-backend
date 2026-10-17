-- Skultem's own business: what each school pays Skultem to use the platform. Completely separate from the fees a
-- school collects from its parents (those live in the school's own fee tables).

CREATE TABLE subscription_plans (
    id uuid PRIMARY KEY,
    name character varying(120) NOT NULL UNIQUE,
    description text,
    price numeric(14,2) NOT NULL CHECK (price >= 0),
    currency character varying(3) NOT NULL DEFAULT 'SLE',
    frequency character varying(12) NOT NULL,
    max_students integer CHECK (max_students IS NULL OR max_students > 0),
    active boolean NOT NULL DEFAULT true,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT subscription_plans_frequency_check CHECK (frequency IN ('MONTHLY', 'QUARTERLY', 'ANNUAL'))
);

-- A school's subscription. price/currency/frequency are copied from the plan when it starts (and the price can be
-- negotiated), so changing a plan later never changes what an existing school owes.
CREATE TABLE school_subscriptions (
    id uuid PRIMARY KEY,
    school_id character varying(255) NOT NULL REFERENCES schools (id),
    plan_id uuid NOT NULL REFERENCES subscription_plans (id),
    price numeric(14,2) NOT NULL CHECK (price >= 0),
    currency character varying(3) NOT NULL,
    frequency character varying(12) NOT NULL,
    start_date date NOT NULL,
    -- The end of the latest period that has been invoiced.
    current_period_end date NOT NULL,
    cancelled_at timestamp(6) with time zone,
    cancel_reason text,
    notes text,
    created_by character varying(255),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT school_subscriptions_frequency_check CHECK (frequency IN ('MONTHLY', 'QUARTERLY', 'ANNUAL'))
);
-- A school has at most one subscription that has not been cancelled.
CREATE UNIQUE INDEX uq_school_live_subscription ON school_subscriptions (school_id) WHERE cancelled_at IS NULL;
CREATE INDEX idx_school_subscriptions_period_end ON school_subscriptions (current_period_end);

CREATE SEQUENCE subscription_invoice_number_seq;
CREATE SEQUENCE subscription_receipt_number_seq;

CREATE TABLE subscription_invoices (
    id uuid PRIMARY KEY,
    number character varying(30) NOT NULL UNIQUE,
    subscription_id uuid NOT NULL REFERENCES school_subscriptions (id),
    period_start date NOT NULL,
    period_end date NOT NULL,
    amount numeric(14,2) NOT NULL CHECK (amount >= 0),
    due_date date NOT NULL,
    status character varying(10) NOT NULL DEFAULT 'OPEN',
    issued_at timestamp(6) with time zone,
    voided_at timestamp(6) with time zone,
    void_reason text,
    created_by character varying(255),
    CONSTRAINT subscription_invoices_status_check CHECK (status IN ('OPEN', 'VOID')),
    CONSTRAINT subscription_invoices_period_check CHECK (period_end >= period_start)
);
CREATE INDEX idx_subscription_invoices_subscription ON subscription_invoices (subscription_id, period_start);

-- A payment is a record somebody at Skultem made of money actually received (with its reference); it is never
-- created automatically. A mistaken one is voided with a reason, never deleted, so the history stays complete.
CREATE TABLE subscription_payments (
    id uuid PRIMARY KEY,
    receipt_number character varying(30) NOT NULL UNIQUE,
    invoice_id uuid NOT NULL REFERENCES subscription_invoices (id),
    amount numeric(14,2) NOT NULL CHECK (amount > 0),
    method character varying(20) NOT NULL,
    reference character varying(255),
    paid_at timestamp(6) with time zone NOT NULL,
    note text,
    recorded_by character varying(255),
    recorded_at timestamp(6) with time zone,
    voided_at timestamp(6) with time zone,
    voided_by character varying(255),
    void_reason text,
    CONSTRAINT subscription_payments_method_check CHECK (method IN ('CASH', 'BANK_TRANSFER', 'MOBILE_MONEY', 'OTHER'))
);
CREATE INDEX idx_subscription_payments_invoice ON subscription_payments (invoice_id);
CREATE INDEX idx_subscription_payments_paid_at ON subscription_payments (paid_at DESC);
