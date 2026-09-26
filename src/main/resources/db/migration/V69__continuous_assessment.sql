-- Continuous Assessment (CA). How a management section assesses its students is an ADMIN setting
-- (assessment_configurations); teachers record scores using it (assessment_ca_entries feed the existing
-- assessment_scores.score); and every assessment freezes the structure it was opened with, so changing the
-- configuration later only affects assessments that open afterwards.

-- 1. The configuration: one row per management section, plus optionally one school-wide row (management_section_id
-- null) that covers sections without their own and schools without sections. No row = SIMPLE, i.e. exactly how
-- assessments have always worked.
CREATE TABLE public.assessment_configurations (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    management_section_id character varying(255),
    structure character varying(20) NOT NULL,
    ca_percentage integer NOT NULL DEFAULT 0,
    formal_percentage integer NOT NULL DEFAULT 100,
    ca_frequency character varying(20),
    ca_entries integer NOT NULL DEFAULT 0,
    version integer NOT NULL DEFAULT 1,
    updated_by_user_id character varying(255),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT assessment_configurations_pkey PRIMARY KEY (id),
    CONSTRAINT assessment_configurations_structure_check CHECK (structure IN ('SIMPLE', 'CA_AND_TEST')),
    CONSTRAINT assessment_configurations_frequency_check CHECK (ca_frequency IS NULL OR ca_frequency IN ('DAILY', 'WEEKLY', 'MID_WEEK', 'CUSTOM')),
    CONSTRAINT assessment_configurations_school_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id),
    CONSTRAINT assessment_configurations_section_fkey FOREIGN KEY (management_section_id)
        REFERENCES public.management_sections(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX uq_assessment_config_school ON public.assessment_configurations (school_id)
    WHERE management_section_id IS NULL;
CREATE UNIQUE INDEX uq_assessment_config_section ON public.assessment_configurations (management_section_id)
    WHERE management_section_id IS NOT NULL;

-- 2. What each assessment (one per class subject + term + test) was opened with. Null structure_frozen_at = not
-- opened yet, so it will take the configuration in force when it opens. Everything that already exists is
-- frozen as SIMPLE - it was recorded that way and must never change.
ALTER TABLE public.class_subject_assessment_life_cycle
    ADD COLUMN structure character varying(20),
    ADD COLUMN ca_percentage integer,
    ADD COLUMN formal_percentage integer,
    ADD COLUMN ca_frequency character varying(20),
    ADD COLUMN ca_entries integer,
    ADD COLUMN config_version integer,
    ADD COLUMN structure_frozen_at timestamp(6) with time zone;

UPDATE public.class_subject_assessment_life_cycle
SET structure = 'SIMPLE', ca_percentage = 0, formal_percentage = 100, ca_entries = 0, structure_frozen_at = now();

-- 3. The two parts of a CA-structured score. score stays the combined 0-100 value everything downstream
-- (weighted totals, approval, report cards) already reads.
ALTER TABLE public.assessment_scores
    ADD COLUMN ca_score integer,
    ADD COLUMN formal_score integer;

CREATE TABLE public.assessment_ca_entries (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    assessment_score_id character varying(255) NOT NULL,
    entry_number integer NOT NULL,
    score integer NOT NULL,
    recorded_by_user_id character varying(255),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT assessment_ca_entries_pkey PRIMARY KEY (id),
    CONSTRAINT assessment_ca_entries_score_check CHECK (score >= 0 AND score <= 100),
    CONSTRAINT assessment_ca_entries_entry_check CHECK (entry_number >= 1),
    CONSTRAINT assessment_ca_entries_score_fkey FOREIGN KEY (assessment_score_id) REFERENCES public.assessment_scores(id)
);
CREATE UNIQUE INDEX uq_assessment_ca_entry ON public.assessment_ca_entries (assessment_score_id, entry_number);
CREATE INDEX idx_assessment_ca_entries_school ON public.assessment_ca_entries (school_id);
