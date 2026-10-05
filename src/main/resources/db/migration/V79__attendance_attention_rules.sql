ALTER TABLE public.schools
    ADD COLUMN attendance_window_days integer NOT NULL DEFAULT 20,
    ADD COLUMN attendance_min_days integer NOT NULL DEFAULT 5,
    ADD COLUMN attendance_streak_days integer NOT NULL DEFAULT 3;
