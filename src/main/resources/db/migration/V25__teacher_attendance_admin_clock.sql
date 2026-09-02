-- Marks a clock-in/clock-out that an admin recorded on a teacher's behalf (e.g. an internet or
-- GPS issue on the teacher's end), as opposed to the teacher's own geofenced self-service Clock
-- In/Out - see AdminClockInUseCase/AdminClockOutUseCase. Both default false: existing rows and
-- every self-service clock stay "not admin-assisted".
ALTER TABLE public.teacher_attendances ADD COLUMN clock_in_by_admin boolean NOT NULL DEFAULT false;
ALTER TABLE public.teacher_attendances ADD COLUMN clock_out_by_admin boolean NOT NULL DEFAULT false;
