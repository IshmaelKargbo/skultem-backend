-- Continuous Assessment is configured per assessment template (a school can use CA for its JSS template and plain
-- scores for primary). A template's own configuration wins; assessments of a template without one keep following the
-- earlier section / school-wide configuration, so nothing changes until a template is set up.
ALTER TABLE assessment_configurations ADD COLUMN assessment_template_id character varying(255);
ALTER TABLE assessment_configurations ADD CONSTRAINT assessment_configurations_template_fkey
    FOREIGN KEY (assessment_template_id) REFERENCES assessment_templates(id) ON DELETE CASCADE;

DROP INDEX IF EXISTS uq_assessment_config_school;
DROP INDEX IF EXISTS uq_assessment_config_section;
CREATE UNIQUE INDEX uq_assessment_config_school ON assessment_configurations (school_id)
    WHERE management_section_id IS NULL AND assessment_template_id IS NULL;
CREATE UNIQUE INDEX uq_assessment_config_section ON assessment_configurations (management_section_id)
    WHERE management_section_id IS NOT NULL AND assessment_template_id IS NULL;
CREATE UNIQUE INDEX uq_assessment_config_template ON assessment_configurations (assessment_template_id)
    WHERE assessment_template_id IS NOT NULL;
