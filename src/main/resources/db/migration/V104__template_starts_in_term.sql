-- Where students start being graded is a setting of the assessment template (a template for a class that joins in the
-- second term), not of each class. Carry over anything set on a class, then drop the class column.
ALTER TABLE assessment_templates ADD COLUMN starts_in_term integer NOT NULL DEFAULT 1;

UPDATE assessment_templates t
SET starts_in_term = COALESCE((SELECT MAX(c.starts_in_term) FROM classes c WHERE c.assessment_template_id = t.id), 1);

ALTER TABLE classes DROP COLUMN starts_in_term;
