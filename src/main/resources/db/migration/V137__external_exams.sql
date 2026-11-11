-- External (national) exams: NPSE, BECE, WASSCE and any a school adds. See ExternalExam, ExternalExamSitting,
-- ExternalExamCandidate. Grade scales, a sitting's subjects and a candidate's scores are JSON in text columns,
-- like report card subjects: they are read and written whole, never queried inside.

CREATE TABLE IF NOT EXISTS external_exams (
    id varchar(255) PRIMARY KEY,
    school_id varchar(255) NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    code varchar(32) NOT NULL,
    name varchar(255) NOT NULL,
    exam_body varchar(255),
    level varchar(32),
    grade_scale text NOT NULL,
    aggregate_mode varchar(16) NOT NULL CHECK (aggregate_mode IN ('POINTS', 'TOTAL')),
    best_of integer NOT NULL DEFAULT 0,
    built_in boolean NOT NULL DEFAULT false,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT uq_external_exams_school_code UNIQUE (school_id, code)
);

CREATE TABLE IF NOT EXISTS external_exam_sittings (
    id varchar(255) PRIMARY KEY,
    school_id varchar(255) NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    exam_id varchar(255) NOT NULL REFERENCES external_exams(id),
    class_id varchar(255) NOT NULL REFERENCES classes(id),
    academic_year_id varchar(255) NOT NULL,
    centre_number varchar(64),
    subjects text NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('DRAFT', 'CONFIRMED')),
    confirmed_by_name varchar(255),
    confirmed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT uq_external_exam_sittings UNIQUE (school_id, exam_id, class_id, academic_year_id)
);
CREATE INDEX IF NOT EXISTS idx_external_exam_sittings_school ON external_exam_sittings(school_id);

CREATE TABLE IF NOT EXISTS external_exam_candidates (
    id varchar(255) PRIMARY KEY,
    school_id varchar(255) NOT NULL,
    sitting_id varchar(255) NOT NULL REFERENCES external_exam_sittings(id) ON DELETE CASCADE,
    student_id varchar(255) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    student_name varchar(255),
    admission_number varchar(255),
    index_number varchar(64),
    scores text NOT NULL DEFAULT '{}',
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT uq_external_exam_candidates UNIQUE (sitting_id, student_id)
);
CREATE INDEX IF NOT EXISTS idx_external_exam_candidates_sitting ON external_exam_candidates(sitting_id);
