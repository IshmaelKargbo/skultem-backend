-- One staff (payroll) record per person per school.
--
-- Adding an existing account through "Add Teacher" used to create a second `teachers` row for
-- someone already on staff (CreateTeacherUseCase now refuses that). Two rows meant two salaries,
-- two payslips in every payroll run (the run total - and the PAYROLL expense written to
-- `transactions` when it was generated - counted them twice), and a crash wherever the app looks
-- up "my" staff record (clock-in, my payslips, attendance).
--
-- For every person with more than one record in a school this keeps the OLDEST record and moves
-- everything onto it. The duplicate's payslips are treated as never paid (paper only): in runs
-- where the kept record already has a payslip, the duplicate's payslip is removed, and for runs
-- already generated/published the extra amount is credited back to `transactions` as a new
-- ADJUSTMENT entry linked to the run. Past entries are never edited - each row stores a running
-- balance, so the adjustment is appended and the balance carries on from the latest entry.
-- Finally a unique index makes a second record impossible.
--
-- Runs inside Flyway's transaction: if anything fails, nothing is changed.

DO $$
DECLARE
    dup RECORD;
    run RECORD;
    last_balance numeric(38, 2);
    year_id varchar(255);
    term_id varchar(255);
    seq int := 0;
BEGIN
    FOR dup IN
        SELECT t.id AS drop_id, k.keep_id, t.school_id
        FROM teachers t
        JOIN (
            SELECT DISTINCT ON (user_id, school_id) user_id, school_id, id AS keep_id
            FROM teachers
            ORDER BY user_id, school_id, created_at ASC NULLS LAST, id ASC
        ) k ON k.user_id = t.user_id AND k.school_id = t.school_id
        WHERE t.id <> k.keep_id
    LOOP
        RAISE NOTICE 'Merging staff record % into %', dup.drop_id, dup.keep_id;

        -- Payroll: the duplicate's payslips in runs the kept record is already on were never owed.
        -- Credit generated/published runs back, then remove those payslips (their lines cascade).
        FOR run IN
            SELECT r.id AS run_id, r.status,
                   SUM(p.basic_salary + p.allowances - p.deductions) FILTER (WHERE p.included) AS extra
            FROM payslips p
            JOIN payroll_runs r ON r.id = p.payroll_run_id
            WHERE p.teacher_id = dup.drop_id
              AND EXISTS (SELECT 1 FROM payslips k
                          WHERE k.payroll_run_id = p.payroll_run_id AND k.teacher_id = dup.keep_id)
            GROUP BY r.id, r.status
        LOOP
            IF run.status <> 'DRAFT' AND COALESCE(run.extra, 0) > 0 THEN
                -- Same year/term the run's original PAYROLL entry was booked to.
                SELECT tr.academic_year_id, tr.term_id INTO year_id, term_id
                FROM transactions tr
                WHERE tr.reference_id = run.run_id AND tr.transaction_type = 'PAYROLL'
                ORDER BY tr.created_at ASC
                LIMIT 1;

                IF year_id IS NOT NULL THEN
                    SELECT tr.balance INTO last_balance
                    FROM transactions tr
                    WHERE tr.school_id = dup.school_id
                    ORDER BY tr.created_at DESC
                    LIMIT 1;

                    seq := seq + 1;
                    INSERT INTO transactions (id, amount, balance, created_at, direction, reference_id,
                                              reference_type, school_id, transaction_type, academic_year_id, term_id)
                    VALUES (gen_random_uuid()::text, run.extra, COALESCE(last_balance, 0) + run.extra,
                            -- a hair apart so "latest entry" (and so the running balance) stays in order
                            now() + (seq * interval '1 millisecond'),
                            'CREDIT', run.run_id, 'PAYROLL', dup.school_id, 'ADJUSTMENT', year_id, term_id);
                END IF;
            END IF;

            DELETE FROM payslips WHERE payroll_run_id = run.run_id AND teacher_id = dup.drop_id;
        END LOOP;

        -- Any of the duplicate's payslips left are in runs the kept record isn't on - keep them.
        UPDATE payslips SET teacher_id = dup.keep_id WHERE teacher_id = dup.drop_id;

        -- One salary per person: the kept record's salary stays (lines cascade with the other).
        IF EXISTS (SELECT 1 FROM salary_structures WHERE teacher_id = dup.keep_id) THEN
            DELETE FROM salary_structures WHERE teacher_id = dup.drop_id;
        ELSE
            UPDATE salary_structures SET teacher_id = dup.keep_id WHERE teacher_id = dup.drop_id;
        END IF;

        -- Attendance: one row per day - keep the kept record's where both clocked the same day.
        DELETE FROM teacher_attendances d
        WHERE d.teacher_id = dup.drop_id
          AND EXISTS (SELECT 1 FROM teacher_attendances k
                      WHERE k.teacher_id = dup.keep_id AND k.school_id = d.school_id AND k.date = d.date);
        UPDATE teacher_attendances SET teacher_id = dup.keep_id WHERE teacher_id = dup.drop_id;

        -- Everything else simply moves across.
        UPDATE teacher_subjects SET teacher_id = dup.keep_id WHERE teacher_id = dup.drop_id;
        UPDATE class_masters SET teacher_id = dup.keep_id WHERE teacher_id = dup.drop_id;
        UPDATE leave_requests SET teacher_id = dup.keep_id WHERE teacher_id = dup.drop_id;
        UPDATE library_loans SET teacher_id = dup.keep_id WHERE teacher_id = dup.drop_id;
        UPDATE houses_house_masters SET house_masters_id = dup.keep_id WHERE house_masters_id = dup.drop_id;

        DELETE FROM teachers WHERE id = dup.drop_id;
    END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_teachers_user_school ON public.teachers (user_id, school_id);
