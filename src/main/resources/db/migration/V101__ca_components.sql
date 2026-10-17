-- Continuous Assessment components. A section can split the CA part of an assessment into named, weighted parts
-- (e.g. Test 1 = Attendance 20 + Assignment 20 + Class participation 20 + Test 40) instead of N equal recordings.
-- One line per component: name|weight (weight = % of the whole assessment). Component n is CA recording no. n.
-- formal_label names the formal part ("Test", "Formal test"). Both are copied onto each assessment when it opens.
ALTER TABLE assessment_configurations
    ADD COLUMN ca_components_text TEXT,
    ADD COLUMN formal_label character varying(60);

ALTER TABLE class_subject_assessment_life_cycle
    ADD COLUMN ca_components_text TEXT,
    ADD COLUMN formal_label character varying(60);
