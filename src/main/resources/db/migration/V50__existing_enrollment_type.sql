-- New admission type for a student who already attends the school (not a new admission, not a
-- transfer from elsewhere) but is only now being entered into the system - previous school and
-- last class don't apply to them.
ALTER TABLE students DROP CONSTRAINT students_enrollment_type_check;
ALTER TABLE students ADD CONSTRAINT students_enrollment_type_check
    CHECK (((enrollment_type)::text = ANY ((ARRAY['NEW'::character varying, 'TRANSFER'::character varying, 'RE_ENROLLMENT'::character varying, 'EXISTING'::character varying])::text[])));
