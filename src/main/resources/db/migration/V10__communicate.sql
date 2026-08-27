-- Notices, calendar events/holidays and broadcasts for the Communicate feature.

CREATE TABLE public.notices (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    content text NOT NULL,
    category character varying(255) NOT NULL,
    audience character varying(255) NOT NULL,
    pinned boolean NOT NULL,
    posted_by_user_id character varying(255) NOT NULL,
    posted_by_name character varying(255) NOT NULL,
    expires_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT notices_pkey PRIMARY KEY (id),
    CONSTRAINT notices_category_check CHECK (((category)::text = ANY ((ARRAY['GENERAL'::character varying, 'ACADEMIC'::character varying, 'FEE'::character varying, 'URGENT'::character varying])::text[]))),
    CONSTRAINT notices_audience_check CHECK (((audience)::text = ANY ((ARRAY['ALL'::character varying, 'STUDENTS'::character varying, 'PARENTS'::character varying, 'TEACHERS'::character varying, 'STAFF'::character varying])::text[])))
);

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);

ALTER TABLE ONLY public.notices
    ADD CONSTRAINT notices_posted_by_user_id_fkey FOREIGN KEY (posted_by_user_id) REFERENCES public.users(id);

CREATE INDEX IF NOT EXISTS idx_notices_school_id ON public.notices (school_id);
CREATE INDEX IF NOT EXISTS idx_notices_posted_by_user_id ON public.notices (posted_by_user_id);

CREATE TABLE public.calendar_events (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    description text NOT NULL,
    type character varying(255) NOT NULL,
    start_date timestamp(6) with time zone NOT NULL,
    end_date timestamp(6) with time zone NOT NULL,
    location character varying(255),
    created_by_user_id character varying(255) NOT NULL,
    created_by_name character varying(255) NOT NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT calendar_events_pkey PRIMARY KEY (id),
    CONSTRAINT calendar_events_type_check CHECK (((type)::text = ANY ((ARRAY['EVENT'::character varying, 'HOLIDAY'::character varying])::text[])))
);

ALTER TABLE ONLY public.calendar_events
    ADD CONSTRAINT calendar_events_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);

ALTER TABLE ONLY public.calendar_events
    ADD CONSTRAINT calendar_events_created_by_user_id_fkey FOREIGN KEY (created_by_user_id) REFERENCES public.users(id);

CREATE INDEX IF NOT EXISTS idx_calendar_events_school_id ON public.calendar_events (school_id);
CREATE INDEX IF NOT EXISTS idx_calendar_events_created_by_user_id ON public.calendar_events (created_by_user_id);

CREATE TABLE public.broadcasts (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    message text NOT NULL,
    audience character varying(255) NOT NULL,
    channels character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    recipients_count integer NOT NULL,
    delivered_count integer NOT NULL,
    sent_by_user_id character varying(255) NOT NULL,
    sent_by_name character varying(255) NOT NULL,
    scheduled_at timestamp(6) with time zone,
    sent_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT broadcasts_pkey PRIMARY KEY (id),
    CONSTRAINT broadcasts_audience_check CHECK (((audience)::text = ANY ((ARRAY['ALL'::character varying, 'STUDENTS'::character varying, 'PARENTS'::character varying, 'TEACHERS'::character varying, 'STAFF'::character varying])::text[]))),
    CONSTRAINT broadcasts_status_check CHECK (((status)::text = ANY ((ARRAY['SENT'::character varying, 'SCHEDULED'::character varying, 'FAILED'::character varying])::text[])))
);

ALTER TABLE ONLY public.broadcasts
    ADD CONSTRAINT broadcasts_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);

ALTER TABLE ONLY public.broadcasts
    ADD CONSTRAINT broadcasts_sent_by_user_id_fkey FOREIGN KEY (sent_by_user_id) REFERENCES public.users(id);

CREATE INDEX IF NOT EXISTS idx_broadcasts_school_id ON public.broadcasts (school_id);
CREATE INDEX IF NOT EXISTS idx_broadcasts_sent_by_user_id ON public.broadcasts (sent_by_user_id);
