-- An exam's own print designs (M-Compose template references). Null = the school's general exam design
-- (school_document_designs EXAM_RESULT_SLIP / EXAM_STATEMENT), else the server default.
ALTER TABLE external_exams ADD COLUMN IF NOT EXISTS slip_template varchar(255);
ALTER TABLE external_exams ADD COLUMN IF NOT EXISTS statement_template varchar(255);
