-- A class's own weekly-plan sessions (e.g. Nursery 2 a day, Primary 4 a day). Kept on the class, so every section
-- of it uses them year after year. Classes without a row use the school's (weekly_plan_settings).
CREATE TABLE weekly_plan_class_slots (
    class_id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    slots text NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT weekly_plan_class_slots_pkey PRIMARY KEY (class_id)
);

-- A weekly plan covers a whole school term (all its weeks), not one month.
ALTER TABLE weekly_plans ADD COLUMN term_id character varying(255);
ALTER TABLE weekly_plans ALTER COLUMN month DROP NOT NULL;
ALTER TABLE weekly_plans DROP CONSTRAINT uq_weekly_plans_session_month;
DELETE FROM weekly_plans WHERE term_id IS NULL; -- month plans from the first version (never published widely)
ALTER TABLE weekly_plans ALTER COLUMN term_id SET NOT NULL;
ALTER TABLE weekly_plans ADD CONSTRAINT uq_weekly_plans_session_term UNIQUE (class_session_id, term_id);
DROP INDEX IF EXISTS idx_weekly_plans_school;
CREATE INDEX idx_weekly_plans_school ON weekly_plans (school_id, term_id);
