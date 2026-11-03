-- CA recording slots that an administrator has unlocked at least once after they were locked ("2,4"). Kept so the
-- approver can see a recording was changed after it was closed; the reason is on the audit trail.
ALTER TABLE class_subject_assessment_life_cycle ADD COLUMN ca_unlocked_weeks VARCHAR(255);
