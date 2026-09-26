-- Announcements for one management section. A section-limited Admin (e.g. Primary) can post a notice,
-- calendar event or broadcast that only that section's people see; null = the whole school (what every
-- existing row is, so nothing changes for a school without sections). Removing a section removes its own
-- announcements rather than quietly widening them to the whole school.
ALTER TABLE public.notices ADD COLUMN management_section_id character varying(255);
ALTER TABLE public.calendar_events ADD COLUMN management_section_id character varying(255);
ALTER TABLE public.broadcasts ADD COLUMN management_section_id character varying(255);

ALTER TABLE public.notices ADD CONSTRAINT notices_management_section_fkey
    FOREIGN KEY (management_section_id) REFERENCES public.management_sections(id) ON DELETE CASCADE;
ALTER TABLE public.calendar_events ADD CONSTRAINT calendar_events_management_section_fkey
    FOREIGN KEY (management_section_id) REFERENCES public.management_sections(id) ON DELETE CASCADE;
ALTER TABLE public.broadcasts ADD CONSTRAINT broadcasts_management_section_fkey
    FOREIGN KEY (management_section_id) REFERENCES public.management_sections(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_notices_management_section ON public.notices (management_section_id);
CREATE INDEX IF NOT EXISTS idx_calendar_events_management_section ON public.calendar_events (management_section_id);
CREATE INDEX IF NOT EXISTS idx_broadcasts_management_section ON public.broadcasts (management_section_id);
