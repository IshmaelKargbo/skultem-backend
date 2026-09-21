-- Which optional feature modules each school has installed (see FeatureModule). Core features
-- (students, classes, fees, ...) are always on and have no row here. A disabled module keeps its
-- row (enabled = false) rather than being deleted, so re-installing restores it as it was.
CREATE TABLE school_modules (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    module_key character varying(64) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    installed_by character varying(255),
    installed_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT school_modules_pkey PRIMARY KEY (id),
    CONSTRAINT uq_school_modules_school_module UNIQUE (school_id, module_key)
);

CREATE INDEX idx_school_modules_school ON school_modules (school_id);

-- Every school that exists today keeps every module: introducing modules must not make anything
-- disappear from a school that already uses it. Only schools created after this start small.
INSERT INTO school_modules (id, school_id, module_key, enabled, installed_at, updated_at)
SELECT gen_random_uuid()::text, s.id, m.module_key, true, now(), now()
FROM schools s
CROSS JOIN (VALUES
    ('GRADING'), ('REPORT_CARDS'), ('CURRICULUM'), ('BEHAVIOUR'), ('TIMETABLE'),
    ('EXPENSES'), ('PAYROLL'), ('STAFF_HR'), ('COMMUNICATION'), ('ID_CARDS'),
    ('ATHLETIC_HOUSES'), ('MATERIALS_AND_SUPPLIES'), ('ANALYTICS')
) AS m(module_key);
