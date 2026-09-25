-- Clock-in location per management section. A school run from different places needs a geofence
-- for each. management_section_id null = the school-wide location (what every existing row is), so
-- nothing changes for a school that doesn't set a section's own.
ALTER TABLE public.attendance_location_settings
    DROP CONSTRAINT attendance_location_settings_school_id_key;

ALTER TABLE public.attendance_location_settings
    ADD COLUMN management_section_id character varying(255);

ALTER TABLE public.attendance_location_settings
    ADD CONSTRAINT attendance_location_settings_section_fkey
        FOREIGN KEY (management_section_id) REFERENCES public.management_sections(id) ON DELETE CASCADE;

-- Still exactly one school-wide row per school, and now at most one row per section.
CREATE UNIQUE INDEX uq_attendance_location_school_default
    ON public.attendance_location_settings (school_id) WHERE management_section_id IS NULL;
CREATE UNIQUE INDEX uq_attendance_location_section
    ON public.attendance_location_settings (management_section_id) WHERE management_section_id IS NOT NULL;
