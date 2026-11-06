-- Some schools take two roll calls a day (e.g. morning and afternoon) and only count a student
-- present when they were there for both. A school picks 1 or 2; a management section can
-- override it (null = use the school's), same pattern as the other attendance rules (V80).
ALTER TABLE schools
    ADD COLUMN attendance_roll_calls integer NOT NULL DEFAULT 1;

ALTER TABLE management_sections
    ADD COLUMN attendance_roll_calls integer;

-- The day's row keeps present/late/excused as the result for the whole day, so every report keeps
-- reading it unchanged. On a two-roll-call day each roll call's own mark is kept here
-- (PRESENT, LATE, EXCUSED or ABSENT; null = not taken yet) and the day is worked out from both.
ALTER TABLE attendance
    ADD COLUMN first_roll character varying(16),
    ADD COLUMN second_roll character varying(16);
