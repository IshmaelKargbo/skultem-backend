-- First Aid (docs/FIRST_AID.md): sick bay visits, the school's first aid rules, and visits turned away by them.
--
--   first_aid_settings      one row per school: limit rules, medication, required fields, who is told, complaints
--   first_aid_visits        each time a student is seen in the sick bay
--   first_aid_turned_away   a visit a "Refuse" limit rule stopped (not a visit, never counted)
--
-- Medication lines, follow-up notes, who was notified and the copy of a limit rule live as JSON on the visit:
-- they're only ever read with it. Names and classes are copied onto the rows (as library_visits does), so
-- history keeps reading correctly after a class change or exit.

-- NURSE: a staff member who runs the sick bay and sees only that part of the portal. See Role.NURSE.
ALTER TABLE public.school_users DROP CONSTRAINT school_users_role_check;
ALTER TABLE public.school_users ADD CONSTRAINT school_users_role_check CHECK (
    role IN ('SYSTEM_ADMIN', 'OWNER', 'PROPRIETOR', 'PRINCIPAL', 'SUPER_ADMIN', 'ADMIN', 'ACCOUNTANT', 'LIBRARIAN',
             'NURSE', 'TEACHER', 'PARENT')
);

-- Notification.Type.HEALTH: a child sent home or referred, or an alert to the principal.
ALTER TABLE public.notifications DROP CONSTRAINT notifications_type_check;
ALTER TABLE public.notifications ADD CONSTRAINT notifications_type_check CHECK (
    type IN ('ATTENDANCE', 'ANNOUNCEMENT', 'REMINDER', 'BEHAVIOUR', 'FEE', 'ASSESSMENT', 'SUPPLY', 'HEALTH')
);

-- ActivityType.FIRST_AID (see V41: an activity type the check doesn't allow rolls the whole action back).
ALTER TABLE public.activities DROP CONSTRAINT activities_type_check;
ALTER TABLE public.activities ADD CONSTRAINT activities_type_check CHECK (
    type IN ('STUDENT', 'TEACHER', 'PARENT', 'USER', 'CLASS', 'SUBJECT', 'GRADE', 'FEES', 'EXPENSE', 'PAYMENT',
             'SCHOOL', 'SETTINGS', 'SUPPLY', 'SALE', 'LIBRARY', 'FIRST_AID')
);

CREATE TABLE public.first_aid_settings (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    -- [{id, name, levels[], maxVisits, period, action, enabled}] - §6.2
    limit_rules jsonb NOT NULL DEFAULT '[]'::jsonb,
    medication_allowed boolean NOT NULL DEFAULT true,
    same_medicine_hours integer,
    same_medicine_action character varying(20) NOT NULL DEFAULT 'REASON',
    max_medication_lines integer,
    require_brought_by boolean NOT NULL DEFAULT false,
    require_temperature boolean NOT NULL DEFAULT false,
    require_treatment boolean NOT NULL DEFAULT true,
    require_notes_on_return boolean NOT NULL DEFAULT false,
    -- Outcomes that notify parents automatically, e.g. ["SENT_HOME","REFERRED"]
    notify_outcomes jsonb NOT NULL DEFAULT '[]'::jsonb,
    allow_manual_notify boolean NOT NULL DEFAULT true,
    notify_principal_on_referral boolean NOT NULL DEFAULT true,
    principal_alert_visits integer,
    principal_alert_period character varying(20),
    long_stay_minutes integer,
    -- {hidden[], urgent[], custom[{code, label, urgent, hidden}]} - §6.6
    complaints jsonb NOT NULL DEFAULT '{}'::jsonb,
    updated_by character varying(255),
    updated_by_name character varying(255),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT first_aid_settings_pkey PRIMARY KEY (id),
    CONSTRAINT first_aid_settings_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id),
    CONSTRAINT uq_first_aid_settings_school UNIQUE (school_id)
);

CREATE TABLE public.first_aid_visits (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    student_name character varying(255) NOT NULL,
    student_class character varying(255),
    status character varying(20) NOT NULL,
    arrived_at timestamp(6) with time zone NOT NULL,
    -- When the student's school day ends; an open visit after this needs following up.
    school_day_ends_at timestamp(6) with time zone NOT NULL,
    -- [{code, label, urgent}] as they were when chosen
    complaints jsonb NOT NULL,
    -- ",HEADACHE,FEVER," - the codes again, so lists can filter by complaint with a plain LIKE
    complaint_codes character varying(1000) NOT NULL,
    complaint_other character varying(500),
    brought_by character varying(255),
    temperature numeric(4, 1),
    notes character varying(2000),
    treatment character varying(2000),
    -- [{id, name, dose, givenAt, recordedBy, recordedByName, repeatReason}]
    medications jsonb NOT NULL DEFAULT '[]'::jsonb,
    continues_visit_id character varying(255),
    -- The limit rule this visit was allowed past, as it was: {ruleName, maxVisits, period, action, count, urgent, reason, by, byName}
    over_limit jsonb,
    opened_by character varying(255) NOT NULL,
    opened_by_name character varying(255),
    outcome character varying(30),
    left_at timestamp(6) with time zone,
    collected_by character varying(255),
    collected_by_relation character varying(255),
    referred_to character varying(255),
    ambulance_called boolean,
    accompanied_by character varying(255),
    closed_at timestamp(6) with time zone,
    closed_by character varying(255),
    closed_by_name character varying(255),
    -- Set when a visit nobody closed was followed up after the school day: no departure time is recorded.
    follow_up_reason character varying(500),
    -- [{id, text, at, by, byName}] - append-only
    follow_up_notes jsonb NOT NULL DEFAULT '[]'::jsonb,
    -- [{userId, name, at, manual}] and whether a hand notification was already used
    parent_notifications jsonb NOT NULL DEFAULT '[]'::jsonb,
    parents_notified_by_hand boolean NOT NULL DEFAULT false,
    -- NONE (no parent with an account), FAILED (sending failed) or null
    notify_problem character varying(20),
    voided_at timestamp(6) with time zone,
    voided_by character varying(255),
    voided_by_name character varying(255),
    void_reason character varying(500),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT first_aid_visits_pkey PRIMARY KEY (id),
    CONSTRAINT first_aid_visits_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id),
    CONSTRAINT first_aid_visits_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id),
    CONSTRAINT first_aid_visits_continues_fkey FOREIGN KEY (continues_visit_id) REFERENCES public.first_aid_visits(id),
    CONSTRAINT first_aid_visits_status_check CHECK (status IN ('OPEN', 'CLOSED', 'VOIDED')),
    CONSTRAINT first_aid_visits_outcome_check CHECK (outcome IS NULL OR outcome IN ('RETURNED_TO_CLASS', 'SENT_HOME', 'REFERRED')),
    CONSTRAINT first_aid_visits_temperature_check CHECK (temperature IS NULL OR temperature BETWEEN 30.0 AND 45.0),
    -- A closed visit says how it ended; a departure time never comes before arrival.
    CONSTRAINT first_aid_visits_closed_check CHECK (
        status <> 'CLOSED' OR (outcome IS NOT NULL AND closed_at IS NOT NULL
            AND (left_at IS NULL OR left_at >= arrived_at)
            AND (left_at IS NOT NULL OR follow_up_reason IS NOT NULL))
    ),
    CONSTRAINT first_aid_visits_voided_check CHECK (status <> 'VOIDED' OR (voided_at IS NOT NULL AND void_reason IS NOT NULL))
);

-- One open visit per student per school - enforced here, so two visits started at the same moment can't both land.
CREATE UNIQUE INDEX uq_first_aid_visits_one_open ON public.first_aid_visits (school_id, student_id) WHERE status = 'OPEN';
CREATE INDEX idx_first_aid_visits_school_arrived ON public.first_aid_visits (school_id, arrived_at);
CREATE INDEX idx_first_aid_visits_student ON public.first_aid_visits (school_id, student_id, arrived_at);

CREATE TABLE public.first_aid_turned_away (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    student_id character varying(255) NOT NULL,
    student_name character varying(255) NOT NULL,
    student_class character varying(255),
    turned_away_at timestamp(6) with time zone NOT NULL,
    complaints character varying(1000) NOT NULL,
    rule_name character varying(255) NOT NULL,
    max_visits integer NOT NULL,
    period character varying(20) NOT NULL,
    visit_count integer NOT NULL,
    recorded_by character varying(255) NOT NULL,
    recorded_by_name character varying(255),
    created_at timestamp(6) with time zone,
    CONSTRAINT first_aid_turned_away_pkey PRIMARY KEY (id),
    CONSTRAINT first_aid_turned_away_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id),
    CONSTRAINT first_aid_turned_away_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id)
);

CREATE INDEX idx_first_aid_turned_away_school_at ON public.first_aid_turned_away (school_id, turned_away_at);
CREATE INDEX idx_first_aid_turned_away_student ON public.first_aid_turned_away (school_id, student_id, turned_away_at);
