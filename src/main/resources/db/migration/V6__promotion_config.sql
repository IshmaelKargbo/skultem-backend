-- A school's promotion rules: minimum pass mark (guidance shown to class masters, not enforced
-- automatically - there is no per-student average/ranking system yet), how many times a student
-- may repeat the same class before it's flagged, whether admin approval is required at all before
-- a class master's promotion takes effect, and whether a remark is mandatory for every decision
-- (not just repeats). One row per school, created with defaults on first read.

CREATE TABLE public.promotion_configs (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    min_pass_mark integer,
    max_repeat_count integer NOT NULL DEFAULT 2,
    require_approval boolean NOT NULL DEFAULT true,
    require_remark_for_promote boolean NOT NULL DEFAULT false,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT promotion_configs_pkey PRIMARY KEY (id),
    CONSTRAINT promotion_configs_school_id_key UNIQUE (school_id)
);
