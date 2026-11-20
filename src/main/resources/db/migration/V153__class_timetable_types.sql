-- Which timetable a class runs on: PERIODS (the normal weekly timetable - periods each day, a lesson in each) or
-- WEEKLY_PLAN (the term week by week, a subject in each session of the day - e.g. nursery). A class has one, never
-- both. Kept on the class, so every section of it uses the same one year after year. A class without a row runs on
-- the normal timetable.
CREATE TABLE class_timetable_types (
    class_id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    type character varying(16) NOT NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT class_timetable_types_pkey PRIMARY KEY (class_id),
    CONSTRAINT class_timetable_types_type_check CHECK (type IN ('PERIODS', 'WEEKLY_PLAN'))
);
CREATE INDEX idx_class_timetable_types_school ON class_timetable_types (school_id);

-- Classes that already have a weekly plan with subjects in it and no normal-timetable lessons move to the weekly
-- plan. A class that has both stays on the normal timetable (its weekly plan is kept, just not shown).
INSERT INTO class_timetable_types (class_id, school_id, type, updated_at)
SELECT DISTINCT cs.class_id, cs.school_id, 'WEEKLY_PLAN', now()
FROM weekly_plans wp
JOIN class_sessions cs ON cs.id = wp.class_session_id
WHERE (EXISTS (SELECT 1 FROM weekly_plan_entries e WHERE e.plan_id = wp.id)
       OR EXISTS (SELECT 1 FROM weekly_plan_cycle_entries c WHERE c.plan_id = wp.id))
  AND NOT EXISTS (
      SELECT 1 FROM timetables t
      JOIN periods p ON p.id = t.period_id
      JOIN class_sessions s2 ON s2.id = p.session_id
      WHERE s2.class_id = cs.class_id);
