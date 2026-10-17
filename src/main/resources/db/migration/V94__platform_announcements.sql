-- Messages the system admin wants every user, in every school, to see on their notice board.
CREATE TABLE platform_announcements (
    id uuid PRIMARY KEY,
    title character varying(255) NOT NULL,
    message text NOT NULL,
    severity character varying(20) NOT NULL DEFAULT 'INFO',
    active boolean NOT NULL DEFAULT true,
    starts_at timestamp(6) with time zone,
    ends_at timestamp(6) with time zone,
    created_by character varying(255),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT platform_announcements_severity_check CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL'))
);
CREATE INDEX idx_platform_announcements_active ON platform_announcements (active, starts_at, ends_at);
