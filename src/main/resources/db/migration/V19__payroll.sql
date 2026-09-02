-- Payroll: replaces the previously mock-only HR/Payroll frontend with a real feature, built on
-- top of the existing Teacher entity (this school system has no separate non-teaching "employee"
-- concept yet, so every payroll record ties back to a real teacher).
--
-- salary_structures: one row per teacher, the compensation currently in effect. Editing it only
-- changes future payroll runs - past payslips keep their own snapshot (below), so a raise never
-- rewrites payroll history.
CREATE TABLE public.salary_structures (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    teacher_id character varying(255) NOT NULL,
    basic_salary numeric(19,2) NOT NULL,
    allowances numeric(19,2) NOT NULL DEFAULT 0,
    deductions numeric(19,2) NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT salary_structures_pkey PRIMARY KEY (id),
    CONSTRAINT salary_structures_school_teacher_key UNIQUE (school_id, teacher_id)
);

ALTER TABLE ONLY public.salary_structures
    ADD CONSTRAINT salary_structures_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);
ALTER TABLE ONLY public.salary_structures
    ADD CONSTRAINT salary_structures_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES public.teachers(id);

-- payroll_runs: one payroll cycle for a period (e.g. "June 2026"). DRAFT while employees can still
-- be included/excluded, GENERATED once the amounts are locked in, PUBLISHED once payslips are
-- considered official/visible to staff.
CREATE TABLE public.payroll_runs (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    period character varying(255) NOT NULL,
    pay_date date NOT NULL,
    status character varying(50) NOT NULL DEFAULT 'DRAFT',
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT payroll_runs_pkey PRIMARY KEY (id),
    CONSTRAINT payroll_runs_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'GENERATED'::character varying, 'PUBLISHED'::character varying])::text[])))
);

ALTER TABLE ONLY public.payroll_runs
    ADD CONSTRAINT payroll_runs_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);
CREATE INDEX idx_payroll_runs_school_id ON public.payroll_runs (school_id);

-- payslips: one line item per teacher per run, a frozen snapshot of their salary_structures row
-- at the moment the run was created - editing salary_structures afterwards never touches these.
CREATE TABLE public.payslips (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    payroll_run_id character varying(255) NOT NULL,
    teacher_id character varying(255) NOT NULL,
    basic_salary numeric(19,2) NOT NULL,
    allowances numeric(19,2) NOT NULL DEFAULT 0,
    deductions numeric(19,2) NOT NULL DEFAULT 0,
    included boolean NOT NULL DEFAULT true,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT payslips_pkey PRIMARY KEY (id),
    CONSTRAINT payslips_run_teacher_key UNIQUE (payroll_run_id, teacher_id)
);

ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);
ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_payroll_run_id_fkey FOREIGN KEY (payroll_run_id) REFERENCES public.payroll_runs(id);
ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES public.teachers(id);
CREATE INDEX idx_payslips_school_run ON public.payslips (school_id, payroll_run_id);
CREATE INDEX idx_payslips_teacher ON public.payslips (teacher_id);
