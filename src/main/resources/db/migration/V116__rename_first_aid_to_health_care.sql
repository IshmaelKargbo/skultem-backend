-- The First Aid module is now called Health Care (docs/HEALTH_CARE.md). V115 created it as first_aid_*; this renames
-- the tables, their constraints and indexes, the stored module key and the activity type. V115 is left as it was
-- because it may already have run on a database - changing an applied migration stops Flyway from starting.

ALTER TABLE public.first_aid_settings RENAME TO health_care_settings;
ALTER TABLE public.health_care_settings RENAME CONSTRAINT first_aid_settings_pkey TO health_care_settings_pkey;
ALTER TABLE public.health_care_settings RENAME CONSTRAINT first_aid_settings_school_id_fkey TO health_care_settings_school_id_fkey;
ALTER TABLE public.health_care_settings RENAME CONSTRAINT uq_first_aid_settings_school TO uq_health_care_settings_school;

ALTER TABLE public.first_aid_visits RENAME TO health_care_visits;
ALTER TABLE public.health_care_visits RENAME CONSTRAINT first_aid_visits_pkey TO health_care_visits_pkey;
ALTER TABLE public.health_care_visits RENAME CONSTRAINT first_aid_visits_school_id_fkey TO health_care_visits_school_id_fkey;
ALTER TABLE public.health_care_visits RENAME CONSTRAINT first_aid_visits_student_id_fkey TO health_care_visits_student_id_fkey;
ALTER TABLE public.health_care_visits RENAME CONSTRAINT first_aid_visits_continues_fkey TO health_care_visits_continues_fkey;
ALTER TABLE public.health_care_visits RENAME CONSTRAINT first_aid_visits_status_check TO health_care_visits_status_check;
ALTER TABLE public.health_care_visits RENAME CONSTRAINT first_aid_visits_outcome_check TO health_care_visits_outcome_check;
ALTER TABLE public.health_care_visits RENAME CONSTRAINT first_aid_visits_temperature_check TO health_care_visits_temperature_check;
ALTER TABLE public.health_care_visits RENAME CONSTRAINT first_aid_visits_closed_check TO health_care_visits_closed_check;
ALTER TABLE public.health_care_visits RENAME CONSTRAINT first_aid_visits_voided_check TO health_care_visits_voided_check;

ALTER TABLE public.first_aid_turned_away RENAME TO health_care_turned_away;
ALTER TABLE public.health_care_turned_away RENAME CONSTRAINT first_aid_turned_away_pkey TO health_care_turned_away_pkey;
ALTER TABLE public.health_care_turned_away RENAME CONSTRAINT first_aid_turned_away_school_id_fkey TO health_care_turned_away_school_id_fkey;
ALTER TABLE public.health_care_turned_away RENAME CONSTRAINT first_aid_turned_away_student_id_fkey TO health_care_turned_away_student_id_fkey;

ALTER INDEX public.uq_first_aid_visits_one_open RENAME TO uq_health_care_visits_one_open;
ALTER INDEX public.idx_first_aid_visits_school_arrived RENAME TO idx_health_care_visits_school_arrived;
ALTER INDEX public.idx_first_aid_visits_student RENAME TO idx_health_care_visits_student;
ALTER INDEX public.idx_first_aid_turned_away_school_at RENAME TO idx_health_care_turned_away_school_at;
ALTER INDEX public.idx_first_aid_turned_away_student RENAME TO idx_health_care_turned_away_student;

-- Schools that installed the module keep it, under its new key (FeatureModule.HEALTH_CARE.key()).
UPDATE public.school_modules SET module_key = 'health-care' WHERE module_key = 'first-aid';

-- ActivityType.FIRST_AID is now HEALTH_CARE: allow both while moving existing rows, then only the new one.
ALTER TABLE public.activities DROP CONSTRAINT activities_type_check;
UPDATE public.activities SET type = 'HEALTH_CARE' WHERE type = 'FIRST_AID';
ALTER TABLE public.activities ADD CONSTRAINT activities_type_check CHECK (
    type IN ('STUDENT', 'TEACHER', 'PARENT', 'USER', 'CLASS', 'SUBJECT', 'GRADE', 'FEES', 'EXPENSE', 'PAYMENT',
             'SCHOOL', 'SETTINGS', 'SUPPLY', 'SALE', 'LIBRARY', 'HEALTH_CARE')
);
