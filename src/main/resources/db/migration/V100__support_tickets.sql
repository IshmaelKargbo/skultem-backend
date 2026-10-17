-- Support tickets: a school (or someone at one) needs help with Skultem. Staff open and work them from the admin
-- portal. Replies are recorded on the ticket - nothing is emailed or texted from here, so a reply is never presented
-- as delivered.
CREATE SEQUENCE support_ticket_number_seq;

CREATE TABLE support_tickets (
    id uuid PRIMARY KEY,
    number character varying(30) NOT NULL UNIQUE,
    school_id character varying(255) REFERENCES schools (id) ON DELETE SET NULL,
    requester_name character varying(255) NOT NULL,
    requester_email character varying(255),
    requester_phone character varying(30),
    category character varying(30) NOT NULL,
    priority character varying(10) NOT NULL DEFAULT 'NORMAL',
    status character varying(15) NOT NULL DEFAULT 'OPEN',
    subject character varying(255) NOT NULL,
    description text NOT NULL,
    assigned_to character varying(255) REFERENCES users (id) ON DELETE SET NULL,
    resolution_note text,
    created_by character varying(255) REFERENCES users (id) ON DELETE SET NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    resolved_at timestamp(6) with time zone,
    CONSTRAINT support_tickets_category_check CHECK (category IN ('ACCOUNT_ACCESS', 'BILLING', 'BUG', 'TRAINING', 'DATA', 'FEATURE_REQUEST', 'OTHER')),
    CONSTRAINT support_tickets_priority_check CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT support_tickets_status_check CHECK (status IN ('OPEN', 'IN_PROGRESS', 'WAITING', 'RESOLVED', 'CLOSED'))
);
CREATE INDEX idx_support_tickets_status ON support_tickets (status, priority);
CREATE INDEX idx_support_tickets_school ON support_tickets (school_id);
CREATE INDEX idx_support_tickets_assigned ON support_tickets (assigned_to);

CREATE TABLE support_ticket_messages (
    id uuid PRIMARY KEY,
    ticket_id uuid NOT NULL REFERENCES support_tickets (id) ON DELETE CASCADE,
    -- REPLY: said to the school (recorded here, sent elsewhere). FROM_SCHOOL: what the school said back.
    -- INTERNAL: a note for staff only. SYSTEM: something the system recorded (a status change).
    kind character varying(15) NOT NULL,
    body text NOT NULL,
    created_by character varying(255) REFERENCES users (id) ON DELETE SET NULL,
    created_at timestamp(6) with time zone,
    CONSTRAINT support_ticket_messages_kind_check CHECK (kind IN ('REPLY', 'FROM_SCHOOL', 'INTERNAL', 'SYSTEM'))
);
CREATE INDEX idx_support_ticket_messages_ticket ON support_ticket_messages (ticket_id, created_at);
