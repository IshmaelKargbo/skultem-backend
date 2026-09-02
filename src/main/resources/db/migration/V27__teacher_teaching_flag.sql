-- Distinguishes a classroom teacher from non-teaching staff/payroll-only records sharing this
-- same table (see V26__teacher_designation.sql) - drives whether the Subjects/Curriculum tabs
-- show on their profile. Defaults true so every existing row (all created through Add Teacher so
-- far) keeps behaving exactly as before; Add Staff and payroll-inclusion-on-Add-User both set it
-- false for the new records they create.
ALTER TABLE public.teachers ADD COLUMN teaching boolean NOT NULL DEFAULT true;
