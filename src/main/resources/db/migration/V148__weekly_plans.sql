-- Weekly plans: a class section's month split into weeks, each school day with a few subjects (one per session
-- slot, e.g. "Morning" and "After break"). Generic for any level; built for nursery classes first.

-- Each school's session slots, in order: [{"name": "Morning", "start": "08:30", "end": "10:00"}, ...].
CREATE TABLE weekly_plan_settings (
    school_id character varying(255) NOT NULL,
    slots text NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT weekly_plan_settings_pkey PRIMARY KEY (school_id)
);

-- One plan per class section per month (YYYY-MM). Parents and Skully only see PUBLISHED ones.
CREATE TABLE weekly_plans (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    class_session_id character varying(255) NOT NULL,
    month character varying(7) NOT NULL,
    status character varying(16) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT weekly_plans_pkey PRIMARY KEY (id),
    CONSTRAINT uq_weekly_plans_session_month UNIQUE (class_session_id, month),
    CONSTRAINT weekly_plans_status_check CHECK (status IN ('DRAFT', 'PUBLISHED'))
);
CREATE INDEX idx_weekly_plans_school ON weekly_plans (school_id, month);

-- The subject in one slot of one day.
CREATE TABLE weekly_plan_entries (
    id character varying(255) NOT NULL,
    plan_id character varying(255) NOT NULL REFERENCES weekly_plans (id) ON DELETE CASCADE,
    day date NOT NULL,
    slot integer NOT NULL,
    subject_id character varying(255) NOT NULL,
    CONSTRAINT weekly_plan_entries_pkey PRIMARY KEY (id),
    CONSTRAINT uq_weekly_plan_entries_slot UNIQUE (plan_id, day, slot)
);
