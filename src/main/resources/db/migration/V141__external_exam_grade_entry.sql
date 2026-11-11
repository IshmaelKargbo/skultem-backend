-- An exam's results can be entered as scores (NPSE) or as the grades the exam body releases (BECE 1-7, WASSCE
-- A1-F9). grades is JSON {subjectId: grade} like scores. Built-in BECE and WASSCE switch to grades.
ALTER TABLE external_exams ADD COLUMN IF NOT EXISTS entry_mode varchar(16) NOT NULL DEFAULT 'SCORES'
    CHECK (entry_mode IN ('SCORES', 'GRADES'));
UPDATE external_exams SET entry_mode = 'GRADES' WHERE built_in AND code IN ('BECE', 'WASSCE');
ALTER TABLE external_exam_candidates ADD COLUMN IF NOT EXISTS grades text NOT NULL DEFAULT '{}';
