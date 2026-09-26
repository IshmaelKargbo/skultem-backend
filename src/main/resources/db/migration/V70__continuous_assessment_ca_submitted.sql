-- Continuous assessment runs in two steps: the CA recordings (all of them - "Week 1".."Week 6") are submitted
-- first, and only then is the formal test entered. ca_submitted_at is when the first step was closed.
ALTER TABLE class_subject_assessment_life_cycle ADD COLUMN ca_submitted_at TIMESTAMP WITH TIME ZONE;
