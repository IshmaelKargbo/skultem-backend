-- How teachers type a CA recording. PERCENT (default): a mark out of 100, as always. WEIGHT: a mark out of what
-- the recording is worth - on a 3% week a teacher enters 2 (out of 3). Either way the stored score stays 0-100, so
-- the CA average, the combined score and report cards are unchanged; only data entry and display differ.
ALTER TABLE assessment_configurations
    ADD COLUMN ca_score_scale character varying(20) NOT NULL DEFAULT 'PERCENT';
ALTER TABLE assessment_configurations
    ADD CONSTRAINT assessment_configurations_score_scale_check CHECK (ca_score_scale IN ('PERCENT', 'WEIGHT'));

-- Copied onto each assessment like the rest of its CA setup. Null (older assessments) = PERCENT.
ALTER TABLE class_subject_assessment_life_cycle ADD COLUMN ca_score_scale character varying(20);
