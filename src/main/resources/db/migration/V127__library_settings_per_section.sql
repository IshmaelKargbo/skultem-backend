-- Each management section (e.g. Primary, Secondary) can run its own library with its own borrowing
-- rules. The row with no section is the school's rules; a section without its own row uses those.
ALTER TABLE public.library_settings ADD COLUMN management_section_id character varying(255);

ALTER TABLE public.library_settings
    ADD CONSTRAINT library_settings_management_section_fkey FOREIGN KEY (management_section_id)
    REFERENCES public.management_sections(id) ON DELETE CASCADE;

ALTER TABLE public.library_settings DROP CONSTRAINT uq_library_settings_school;

CREATE UNIQUE INDEX uq_library_settings_school_wide ON public.library_settings (school_id)
    WHERE management_section_id IS NULL;
CREATE UNIQUE INDEX uq_library_settings_section ON public.library_settings (school_id, management_section_id)
    WHERE management_section_id IS NOT NULL;
