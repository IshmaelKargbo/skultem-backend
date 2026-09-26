-- Each CA recording slot ("Week 1".."Week 6") can be locked once every student has it, so a completed week is not
-- quietly edited afterwards. Stored as a comma-separated list of the locked slot numbers, e.g. '1,2,3'.
ALTER TABLE class_subject_assessment_life_cycle ADD COLUMN ca_locked_weeks VARCHAR(255);
UPDATE class_subject_assessment_life_cycle SET ca_locked_weeks = (
    SELECT string_agg(g::text, ',' ORDER BY g) FROM generate_series(1, ca_entries) g)
WHERE ca_submitted_at IS NOT NULL AND ca_entries IS NOT NULL AND ca_entries > 0;
