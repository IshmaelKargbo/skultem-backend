-- Geofenced clock-in: a school configures its GPS location + an acceptable radius once, and a
-- teacher's "Clock In" press is only honored (marks them PRESENT) if their browser-reported
-- location falls within that radius - see ClockInUseCase. Single row per school, same pattern as
-- receipt_settings/report_card_settings.
CREATE TABLE public.attendance_location_settings (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    radius_meters integer NOT NULL DEFAULT 150,
    -- Optional second factor alongside GPS: a comma-separated list of exact IPs and/or IPv4 CIDR
    -- ranges the school's network is known to use. Blank/null = no IP restriction, GPS only.
    allowed_ips character varying(500),
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT attendance_location_settings_pkey PRIMARY KEY (id),
    CONSTRAINT attendance_location_settings_school_id_key UNIQUE (school_id)
);

ALTER TABLE ONLY public.attendance_location_settings
    ADD CONSTRAINT attendance_location_settings_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);

-- Distinguishes a self-service geofenced clock-in from an admin manually marking the register -
-- both null unless the teacher actually pressed Clock In (an admin-marked row leaves them null).
-- clock_in_ip is recorded on every clock-in regardless of whether an IP allowlist is configured,
-- purely for an admin to audit "who clocked in from where" after the fact.
ALTER TABLE public.teacher_attendances ADD COLUMN clocked_in_at timestamp(6) with time zone;
ALTER TABLE public.teacher_attendances ADD COLUMN clock_in_ip character varying(64);
