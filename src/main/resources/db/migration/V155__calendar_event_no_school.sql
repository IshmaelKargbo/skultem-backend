-- Whether the school is closed on a calendar entry's days, so they don't count as school days.
-- Holidays already on the calendar were entered as days off.
ALTER TABLE calendar_events ADD COLUMN no_school BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE calendar_events SET no_school = TRUE WHERE type = 'HOLIDAY' AND management_section_id IS NULL;
