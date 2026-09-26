-- Per-term, per-assessment plan for a section on continuous assessment: for each (term, assessment name) whether it
-- uses CA + formal test and how many CA recordings it has - a short term or a short test has fewer weeks.
-- One line per entry: termId|assessmentName|usesCa(1/0)|caEntries
ALTER TABLE assessment_configurations ADD COLUMN plan_text TEXT;
