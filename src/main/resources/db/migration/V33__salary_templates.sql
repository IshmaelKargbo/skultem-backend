-- Salary templates: a reusable starting point ("Grade A Teacher", "Support Staff", ...) an admin
-- configures once - a basic salary plus a named, itemized set of allowances/deductions (each
-- either a flat amount or a percentage of basic) - instead of retyping the same lines for every
-- teacher on that grade. Applying one to a teacher copies its lines onto that teacher's
-- salary_structures row at that moment (see SetSalaryStructureUseCase); editing the template
-- afterwards never reaches back into structures already built from it.
CREATE TABLE public.salary_templates (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    basic_salary numeric(19,2) NOT NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT salary_templates_pkey PRIMARY KEY (id)
);

ALTER TABLE ONLY public.salary_templates
    ADD CONSTRAINT salary_templates_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);
CREATE INDEX idx_salary_templates_school_id ON public.salary_templates (school_id);

CREATE TABLE public.salary_template_allowances (
    salary_template_id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(20) NOT NULL,
    value numeric(19,2) NOT NULL,
    CONSTRAINT salary_template_allowances_type_check CHECK (((type)::text = ANY ((ARRAY['FIXED'::character varying, 'PERCENTAGE'::character varying])::text[])))
);

ALTER TABLE ONLY public.salary_template_allowances
    ADD CONSTRAINT salary_template_allowances_template_id_fkey FOREIGN KEY (salary_template_id) REFERENCES public.salary_templates(id) ON DELETE CASCADE;
CREATE INDEX idx_salary_template_allowances_template_id ON public.salary_template_allowances (salary_template_id);

CREATE TABLE public.salary_template_deductions (
    salary_template_id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(20) NOT NULL,
    value numeric(19,2) NOT NULL,
    CONSTRAINT salary_template_deductions_type_check CHECK (((type)::text = ANY ((ARRAY['FIXED'::character varying, 'PERCENTAGE'::character varying])::text[])))
);

ALTER TABLE ONLY public.salary_template_deductions
    ADD CONSTRAINT salary_template_deductions_template_id_fkey FOREIGN KEY (salary_template_id) REFERENCES public.salary_templates(id) ON DELETE CASCADE;
CREATE INDEX idx_salary_template_deductions_template_id ON public.salary_template_deductions (salary_template_id);

-- salary_structures: allowances/deductions move from one lump-sum column each to an itemized list
-- (same shape as the template tables above), plus an optional pointer to the template a structure
-- was built from - denormalized template_name so the label survives the template being renamed or
-- deleted later (ON DELETE SET NULL: losing the template shouldn't corrupt an existing teacher's
-- pay, it just stops crediting it to a template).
CREATE TABLE public.salary_structure_allowances (
    salary_structure_id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(20) NOT NULL,
    value numeric(19,2) NOT NULL,
    CONSTRAINT salary_structure_allowances_type_check CHECK (((type)::text = ANY ((ARRAY['FIXED'::character varying, 'PERCENTAGE'::character varying])::text[])))
);

ALTER TABLE ONLY public.salary_structure_allowances
    ADD CONSTRAINT salary_structure_allowances_structure_id_fkey FOREIGN KEY (salary_structure_id) REFERENCES public.salary_structures(id) ON DELETE CASCADE;
CREATE INDEX idx_salary_structure_allowances_structure_id ON public.salary_structure_allowances (salary_structure_id);

CREATE TABLE public.salary_structure_deductions (
    salary_structure_id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(20) NOT NULL,
    value numeric(19,2) NOT NULL,
    CONSTRAINT salary_structure_deductions_type_check CHECK (((type)::text = ANY ((ARRAY['FIXED'::character varying, 'PERCENTAGE'::character varying])::text[])))
);

ALTER TABLE ONLY public.salary_structure_deductions
    ADD CONSTRAINT salary_structure_deductions_structure_id_fkey FOREIGN KEY (salary_structure_id) REFERENCES public.salary_structures(id) ON DELETE CASCADE;
CREATE INDEX idx_salary_structure_deductions_structure_id ON public.salary_structure_deductions (salary_structure_id);

-- Carry forward any existing lump-sum figures as a single named line each, so a school that
-- already has salary structures set up doesn't lose those amounts from gross/net pay.
INSERT INTO public.salary_structure_allowances (salary_structure_id, name, type, value)
SELECT id, 'Allowance', 'FIXED', allowances FROM public.salary_structures WHERE allowances <> 0;

INSERT INTO public.salary_structure_deductions (salary_structure_id, name, type, value)
SELECT id, 'Deduction', 'FIXED', deductions FROM public.salary_structures WHERE deductions <> 0;

ALTER TABLE public.salary_structures
    DROP COLUMN allowances,
    DROP COLUMN deductions,
    ADD COLUMN template_id character varying(255),
    ADD COLUMN template_name character varying(255);

ALTER TABLE ONLY public.salary_structures
    ADD CONSTRAINT salary_structures_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.salary_templates(id) ON DELETE SET NULL;
