-- Clock Out mirrors Clock In: same geofence/IP checks, its own timestamp + IP for the audit trail.
-- Both null until the teacher actually clocks out (an admin-marked row leaves them null, same as
-- clocked_in_at/clock_in_ip).
ALTER TABLE public.teacher_attendances ADD COLUMN clocked_out_at timestamp(6) with time zone;
ALTER TABLE public.teacher_attendances ADD COLUMN clock_out_ip character varying(64);
