ALTER TABLE public.management_sections
    ADD COLUMN attendance_threshold double precision,
    ADD COLUMN attendance_window_days integer,
    ADD COLUMN attendance_min_days integer,
    ADD COLUMN attendance_streak_days integer;
