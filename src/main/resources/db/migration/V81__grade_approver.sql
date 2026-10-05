ALTER TABLE public.schools
    ADD COLUMN grade_approver character varying(32) NOT NULL DEFAULT 'CLASS_MASTER';

ALTER TABLE public.schools
    ADD CONSTRAINT schools_grade_approver_check CHECK (grade_approver IN ('CLASS_MASTER', 'ADMIN'));

ALTER TABLE public.management_sections
    ADD COLUMN grade_approver character varying(32);

ALTER TABLE public.management_sections
    ADD CONSTRAINT management_sections_grade_approver_check CHECK (grade_approver IS NULL OR grade_approver IN ('CLASS_MASTER', 'ADMIN'));

-- Where an admin approves grades there may be no class master at all, so the request can't insist on one.
ALTER TABLE public.assessment_approval_request ALTER COLUMN class_master_id DROP NOT NULL;
