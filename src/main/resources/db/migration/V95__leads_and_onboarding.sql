-- Leads: one CRM record per enquiry. A lead is made from a demo request or a contact message (never both
-- for the same enquiry: each source row can belong to at most one lead) or added by hand.
CREATE TABLE leads (
    id uuid PRIMARY KEY,
    name character varying(255) NOT NULL,
    school_name character varying(255),
    email character varying(255),
    phone character varying(30),
    city character varying(255),
    source character varying(20) NOT NULL,
    demo_request_id uuid UNIQUE REFERENCES request_demos (id) ON DELETE SET NULL,
    contact_message_id uuid UNIQUE REFERENCES marketing_contact_messages (id) ON DELETE SET NULL,
    status character varying(20) NOT NULL DEFAULT 'NEW',
    demo_status character varying(20) NOT NULL DEFAULT 'NOT_SCHEDULED',
    demo_at timestamp(6) with time zone,
    assigned_to character varying(255) REFERENCES users (id) ON DELETE SET NULL,
    next_follow_up_at timestamp(6) with time zone,
    summary text,
    school_id character varying(255) REFERENCES schools (id) ON DELETE SET NULL,
    converted_at timestamp(6) with time zone,
    lost_reason text,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT leads_source_check CHECK (source IN ('DEMO_REQUEST', 'CONTACT_MESSAGE', 'MANUAL', 'REFERRAL', 'OTHER')),
    CONSTRAINT leads_status_check CHECK (status IN ('NEW', 'CONTACTED', 'DEMO_SCHEDULED', 'DEMO_COMPLETED', 'INTERESTED', 'ONBOARDING', 'CONVERTED', 'LOST')),
    CONSTRAINT leads_demo_status_check CHECK (demo_status IN ('NOT_SCHEDULED', 'SCHEDULED', 'COMPLETED', 'CANCELLED'))
);
CREATE INDEX idx_leads_status ON leads (status, next_follow_up_at);
CREATE INDEX idx_leads_assigned ON leads (assigned_to);
CREATE INDEX idx_leads_email ON leads (lower(email));

CREATE TABLE lead_notes (
    id uuid PRIMARY KEY,
    lead_id uuid NOT NULL REFERENCES leads (id) ON DELETE CASCADE,
    kind character varying(20) NOT NULL,
    body text NOT NULL,
    created_by character varying(255) REFERENCES users (id) ON DELETE SET NULL,
    created_at timestamp(6) with time zone,
    CONSTRAINT lead_notes_kind_check CHECK (kind IN ('NOTE', 'CALL', 'EMAIL', 'MEETING', 'STATUS_CHANGE', 'SYSTEM'))
);
CREATE INDEX idx_lead_notes_lead ON lead_notes (lead_id, created_at DESC);

-- Enquiries already received become leads. (The demo requests' own details are the lead's details.)
INSERT INTO leads (id, name, school_name, email, phone, city, source, demo_request_id, status, summary, created_at, updated_at)
SELECT gen_random_uuid(), d.name, d.school, d.email, d.phone, d.city, 'DEMO_REQUEST', d.id, 'NEW',
       concat_ws(' · ', d.priority, d.message), d.created_at, now()
FROM request_demos d;

INSERT INTO leads (id, name, school_name, email, phone, source, contact_message_id, status, summary, created_at, updated_at)
SELECT gen_random_uuid(), m.name, m.school, m.email, m.phone, 'CONTACT_MESSAGE', m.id, 'NEW', m.challenge, m.created_at, now()
FROM marketing_contact_messages m;

-- Onboarding: the steps a new school goes through from first demo to launch. One per lead. The school it
-- ends up as is linked once it exists.
CREATE TABLE onboardings (
    id uuid PRIMARY KEY,
    lead_id uuid NOT NULL UNIQUE REFERENCES leads (id) ON DELETE CASCADE,
    school_id character varying(255) REFERENCES schools (id) ON DELETE SET NULL,
    assigned_to character varying(255) REFERENCES users (id) ON DELETE SET NULL,
    status character varying(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at timestamp(6) with time zone,
    completed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT onboardings_status_check CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE onboarding_steps (
    id uuid PRIMARY KEY,
    onboarding_id uuid NOT NULL REFERENCES onboardings (id) ON DELETE CASCADE,
    step_key character varying(40) NOT NULL,
    label character varying(255) NOT NULL,
    position integer NOT NULL,
    -- A step that does not apply to this school is switched off rather than deleted.
    applicable boolean NOT NULL DEFAULT true,
    status character varying(20) NOT NULL DEFAULT 'PENDING',
    assigned_to character varying(255) REFERENCES users (id) ON DELETE SET NULL,
    due_date date,
    note text,
    completed_at timestamp(6) with time zone,
    completed_by character varying(255) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT onboarding_steps_status_check CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DONE'))
);
CREATE INDEX idx_onboarding_steps_onboarding ON onboarding_steps (onboarding_id, position);
