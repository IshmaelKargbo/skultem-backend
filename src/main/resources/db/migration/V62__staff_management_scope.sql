-- Which management sections a staff member can work in, per role (e.g. Ama as ADMIN -> "JSS", while
-- Ama as TEACHER stays unscoped). No rows for a (school, user, role) means the whole school - every
-- existing user keeps exactly the access they have today. Only applies in a SECTION_BASED school;
-- OWNER/PROPRIETOR/SUPER_ADMIN are always whole-school regardless.
--
-- Keyed by (school, user, role) rather than the school_users row on purpose: removing a role and
-- granting it again creates a new school_users row, and scope attached to that row would silently
-- reset to "whole school" - a way for someone who can manage roles to widen a colleague's access.
CREATE TABLE public.staff_management_sections (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL,
    role character varying(50) NOT NULL,
    management_section_id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    CONSTRAINT staff_management_sections_pkey PRIMARY KEY (id),
    -- No ON DELETE CASCADE: deleting a section someone is limited to would otherwise leave them with
    -- no rows - i.e. whole-school access. UpdateSchoolStructureUseCase refuses that delete instead.
    CONSTRAINT fk_staff_management_sections_section FOREIGN KEY (management_section_id)
        REFERENCES public.management_sections(id)
);

CREATE UNIQUE INDEX uq_staff_management_sections
    ON public.staff_management_sections (school_id, user_id, role, management_section_id);
CREATE INDEX idx_staff_management_sections_lookup
    ON public.staff_management_sections (school_id, user_id, role);
CREATE INDEX idx_staff_management_sections_section
    ON public.staff_management_sections (management_section_id);
