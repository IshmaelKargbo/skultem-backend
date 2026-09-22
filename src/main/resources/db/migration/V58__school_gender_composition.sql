ALTER TABLE public.schools
    ADD COLUMN gender_composition character varying(20) NOT NULL DEFAULT 'MIXED';
