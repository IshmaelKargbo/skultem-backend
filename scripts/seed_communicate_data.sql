-- Seeds sample Notice / CalendarEvent / Broadcast rows for manual testing of the
-- Communicate feature (Notice Board, Events & Holidays, Broadcast) against a real
-- backend + database, instead of the fake data the frontend used to keep locally.
--
-- This is a one-off dev/test fixture, not a Flyway migration: it targets whatever
-- school + user already exist in the database you run it against, so it must never
-- run automatically against every environment (including production).
--
-- Usage (against the local docker-compose postgres):
--   psql "postgresql://moriba:moriba%402024@localhost:5432/skultem" \
--     -f backend/scripts/seed_communicate_data.sql
--
-- Safe to re-run: existing seed rows (fixed ids below) are left untouched.

DO $$
DECLARE
    v_school   character varying(255);
    v_user     character varying(255);
    v_user_name text;
BEGIN
    -- Prefer a user who administers the chosen school; fall back to any user.
    SELECT s.id INTO v_school FROM public.schools s ORDER BY s.created_at ASC LIMIT 1;

    IF v_school IS NULL THEN
        RAISE EXCEPTION 'No school found — create a school first, then re-run this script.';
    END IF;

    SELECT su.user_id INTO v_user
    FROM public.school_users su
    WHERE su.school_id = v_school
      AND su.role IN ('ADMIN', 'OWNER', 'PROPRIETOR')
    ORDER BY su.created_at ASC
    LIMIT 1;

    IF v_user IS NULL THEN
        SELECT u.id INTO v_user FROM public.users u ORDER BY u.created_at ASC LIMIT 1;
    END IF;

    IF v_user IS NULL THEN
        RAISE EXCEPTION 'No user found — create a user first, then re-run this script.';
    END IF;

    SELECT (u.given_name || ' ' || u.family_name) INTO v_user_name
    FROM public.users u WHERE u.id = v_user;

    RAISE NOTICE 'Seeding communicate data for school % / user % (%)', v_school, v_user, v_user_name;

    -- Notices --------------------------------------------------------------
    INSERT INTO public.notices
        (id, school_id, title, content, category, audience, pinned,
         posted_by_user_id, posted_by_name, expires_at, created_at, updated_at)
    VALUES
        ('seed-notice-1', v_school, 'PTA Meeting — Term 2',
         'All parents are invited to the Term 2 PTA meeting in the main hall to discuss academic progress and upcoming activities.',
         'GENERAL', 'PARENTS', true, v_user, 'School Administration',
         now() + interval '7 days', now() - interval '3 days', now() - interval '3 days'),

        ('seed-notice-2', v_school, 'Fee Payment Deadline Reminder',
         'Term 2 fees are due by the end of this month. Please settle outstanding balances to avoid late fees.',
         'FEE', 'PARENTS', true, v_user, 'Accounts Office',
         now() + interval '14 days', now() - interval '1 days', now() - interval '1 days'),

        ('seed-notice-3', v_school, 'Mid-Term Exam Timetable Released',
         'The Term 2 mid-term examination timetable has been published. Check the academics section for your class schedule.',
         'ACADEMIC', 'ALL', false, v_user, 'Academic Office',
         now() + interval '10 days', now() - interval '5 days', now() - interval '5 days'),

        ('seed-notice-4', v_school, 'Inter-House Sports Day Volunteers Needed',
         'We are looking for staff and parent volunteers to help coordinate this term''s inter-house sports day.',
         'GENERAL', 'STAFF', false, v_user, 'Athletics Department',
         now() - interval '1 days', now() - interval '8 days', now() - interval '8 days'),

        ('seed-notice-5', v_school, 'School Closed — Public Holiday',
         'The school will be closed tomorrow in observance of the public holiday. Classes resume the following day.',
         'URGENT', 'ALL', true, v_user, 'School Administration',
         now() + interval '2 days', now(), now())
    ON CONFLICT (id) DO NOTHING;

    -- Calendar events / holidays --------------------------------------------
    INSERT INTO public.calendar_events
        (id, school_id, title, description, type, start_date, end_date, location,
         created_by_user_id, created_by_name, created_at, updated_at)
    VALUES
        ('seed-event-1', v_school, 'Independence Day', 'National public holiday — no classes.',
         'HOLIDAY', now() + interval '12 days', now() + interval '12 days', NULL,
         v_user, v_user_name, now(), now()),

        ('seed-event-2', v_school, 'Founder''s Day Celebration',
         'Annual celebration marking the founding of the school, with performances and awards.',
         'EVENT', now() + interval '20 days', now() + interval '20 days', 'Main Auditorium',
         v_user, v_user_name, now(), now()),

        ('seed-event-3', v_school, 'Inter-House Sports Competition',
         'Athletics and team sports competition between school houses.',
         'EVENT', now() + interval '28 days', now() + interval '29 days', 'School Sports Field',
         v_user, v_user_name, now(), now()),

        ('seed-event-4', v_school, 'Mid-Term Break', 'School closed for the mid-term break.',
         'HOLIDAY', now() + interval '35 days', now() + interval '39 days', NULL,
         v_user, v_user_name, now(), now()),

        ('seed-event-5', v_school, 'Cultural Day',
         'Students showcase local culture through food, dance and dress.',
         'EVENT', now() - interval '6 days', now() - interval '6 days', 'School Compound',
         v_user, v_user_name, now(), now()),

        ('seed-event-6', v_school, 'Christmas & New Year Break', 'End of year holiday break.',
         'HOLIDAY', now() + interval '60 days', now() + interval '74 days', NULL,
         v_user, v_user_name, now(), now())
    ON CONFLICT (id) DO NOTHING;

    -- Broadcasts -------------------------------------------------------------
    INSERT INTO public.broadcasts
        (id, school_id, title, message, audience, channels, status,
         recipients_count, delivered_count, sent_by_user_id, sent_by_name,
         scheduled_at, sent_at, created_at, updated_at)
    SELECT
        'seed-broadcast-1', v_school, 'Term 2 Resumption Notice',
        'School resumes for Term 2 on Monday. Please ensure all students are in proper uniform.',
        'ALL', 'SMS,PUSH', 'SENT',
        counts.all_count, round(counts.all_count * 0.97)::int,
        v_user, 'School Administration',
        NULL, now() - interval '14 days', now() - interval '14 days', now() - interval '14 days'
    FROM (
        SELECT
            (SELECT count(*) FROM public.students s WHERE s.school_id = v_school)
          + (SELECT count(*) FROM public.parents p WHERE p.school_id = v_school)
          + (SELECT count(*) FROM public.teachers t WHERE t.school_id = v_school)
          + (SELECT count(*) FROM public.school_users su WHERE su.school_id = v_school AND su.role IN ('ADMIN', 'ACCOUNTANT', 'PROPRIETOR', 'OWNER'))
          AS all_count
    ) counts
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO public.broadcasts
        (id, school_id, title, message, audience, channels, status,
         recipients_count, delivered_count, sent_by_user_id, sent_by_name,
         scheduled_at, sent_at, created_at, updated_at)
    SELECT
        'seed-broadcast-2', v_school, 'Fee Deadline Reminder',
        'Reminder: Term 2 fees are due by the end of the month. Contact the accounts office with any questions.',
        'PARENTS', 'SMS,EMAIL', 'SENT',
        counts.parents_count, round(counts.parents_count * 0.94)::int,
        v_user, 'Accounts Office',
        NULL, now() - interval '1 days', now() - interval '1 days', now() - interval '1 days'
    FROM (
        SELECT (SELECT count(*) FROM public.parents p WHERE p.school_id = v_school) AS parents_count
    ) counts
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO public.broadcasts
        (id, school_id, title, message, audience, channels, status,
         recipients_count, delivered_count, sent_by_user_id, sent_by_name,
         scheduled_at, sent_at, created_at, updated_at)
    SELECT
        'seed-broadcast-3', v_school, 'Staff Meeting Tomorrow',
        'All teaching staff are required to attend a briefing in the staff room tomorrow at 3:30pm.',
        'TEACHERS', 'EMAIL,IN_APP', 'SCHEDULED',
        counts.teachers_count, 0,
        v_user, 'School Administration',
        now() + interval '1 days', NULL, now(), now()
    FROM (
        SELECT (SELECT count(*) FROM public.teachers t WHERE t.school_id = v_school) AS teachers_count
    ) counts
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO public.broadcasts
        (id, school_id, title, message, audience, channels, status,
         recipients_count, delivered_count, sent_by_user_id, sent_by_name,
         scheduled_at, sent_at, created_at, updated_at)
    SELECT
        'seed-broadcast-4', v_school, 'SMS Gateway Outage Notice',
        'Exam results release announcement.',
        'ALL', 'SMS', 'FAILED',
        counts.all_count, 0,
        v_user, 'Academic Office',
        NULL, now() - interval '4 days', now() - interval '4 days', now() - interval '4 days'
    FROM (
        SELECT
            (SELECT count(*) FROM public.students s WHERE s.school_id = v_school)
          + (SELECT count(*) FROM public.parents p WHERE p.school_id = v_school)
          + (SELECT count(*) FROM public.teachers t WHERE t.school_id = v_school)
          + (SELECT count(*) FROM public.school_users su WHERE su.school_id = v_school AND su.role IN ('ADMIN', 'ACCOUNTANT', 'PROPRIETOR', 'OWNER'))
          AS all_count
    ) counts
    ON CONFLICT (id) DO NOTHING;

    RAISE NOTICE 'Communicate seed data ready: 5 notices, 6 calendar entries, 4 broadcasts.';
END $$;
