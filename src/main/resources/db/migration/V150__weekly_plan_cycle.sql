-- A weekly plan can repeat: the school plans a few weeks by weekday (e.g. a 4-week month for nursery) and that
-- pattern is spread over every school day of the term (weekly_plan_entries, holidays skipped).
-- cycle_weeks 0 = every week of the term planned on its own.
ALTER TABLE weekly_plans ADD COLUMN cycle_weeks integer NOT NULL DEFAULT 0;

-- The repeating pattern: week of the cycle (1..cycle_weeks), weekday (1 Monday .. 5 Friday), session slot.
CREATE TABLE weekly_plan_cycle_entries (
    plan_id character varying(255) NOT NULL REFERENCES weekly_plans (id) ON DELETE CASCADE,
    week integer NOT NULL,
    weekday integer NOT NULL,
    slot integer NOT NULL,
    subject_id character varying(255) NOT NULL,
    CONSTRAINT weekly_plan_cycle_entries_pkey PRIMARY KEY (plan_id, week, weekday, slot)
);
