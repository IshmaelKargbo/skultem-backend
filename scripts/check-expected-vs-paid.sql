-- Read-only: why "Total Expected" and payments don't line up for one school.
-- Change the school id on the next line, then run the whole file (psql -f, or paste into any SQL client).
\set school_id 'SCL-2026-0002'

WITH s AS (SELECT id FROM schools WHERE id = :'school_id'),
     y AS (SELECT a.id, a.name FROM academic_years a JOIN s ON s.id = a.school_id WHERE a.active)
SELECT '1. active academic year' AS check, (SELECT string_agg(name, ', ') FROM y) AS value
UNION ALL
-- Payments split by: which academic year their fee belongs to, platform fee or not, and whether that fee is
-- actually charged to the student (a student_fees row).
SELECT '2. payments: ' || ay.name || CASE WHEN f.system THEN ' / platform fee' ELSE ' / school fee' END
       || CASE WHEN EXISTS (SELECT 1 FROM student_fees sf WHERE sf.fee_id = p.fee_id AND sf.student_id = p.student_id)
               THEN ' / fee IS on the student''s account' ELSE ' / fee NOT on the student''s account' END,
       to_char(sum(p.amount), 'FM999,999,990.00') || '  (' || count(*) || ' payments)'
FROM payments p JOIN s ON s.id = p.school_id
JOIN fee_structures f ON f.id = p.fee_id JOIN academic_years ay ON ay.id = f.academic_year_id
GROUP BY ay.name, f.system, EXISTS (SELECT 1 FROM student_fees sf WHERE sf.fee_id = p.fee_id AND sf.student_id = p.student_id)
UNION ALL
-- Fees charged to students, by academic year and platform/school fee.
SELECT '3. fees charged: ' || ay.name || CASE WHEN f.system THEN ' / platform fee' ELSE ' / school fee' END,
       to_char(sum(f.amount), 'FM999,999,990.00') || '  (' || count(*) || ' charges)'
FROM student_fees sf JOIN s ON s.id = sf.school_id
JOIN fee_structures f ON f.id = sf.fee_id JOIN academic_years ay ON ay.id = f.academic_year_id
GROUP BY ay.name, f.system
UNION ALL
SELECT '4. discounts recorded', count(*) || ' discount(s)'
FROM fee_discounts d JOIN s ON s.id = d.school_id
UNION ALL
-- The ledger's fee charges this year (what "Total Debit" adds up), split by whether the charge has a matching
-- record of the fee on the student's account (student_fees) - Total Expected is built from those records only.
SELECT '6. ledger charges (active year): ' || CASE WHEN EXISTS (SELECT 1 FROM student_fees sf
            WHERE sf.fee_id = l.reference_id AND sf.student_id = l.student_id)
          THEN 'fee IS on the student''s account' ELSE 'fee NOT on the student''s account' END
       || CASE WHEN f.id IS NULL THEN ' / fee structure missing' WHEN f.system THEN ' / platform fee' ELSE '' END,
       to_char(sum(l.amount), 'FM999,999,990.00') || '  (' || count(*) || ' entries, '
       || count(DISTINCT l.student_id) || ' students)'
FROM student_ledger_entries l JOIN s ON s.id = l.school_id JOIN y ON y.id = l.academic_year_id
LEFT JOIN fee_structures f ON f.id = l.reference_id
WHERE l.transaction_type = 'FEE_ASSINMENT'
GROUP BY 1
UNION ALL
-- The same fee charged to the same student more than once in the ledger.
SELECT '7. duplicate ledger charges (same student + fee, active year)',
       coalesce(to_char(sum(extra), 'FM999,999,990.00'), '0.00') || '  (' || count(*) || ' student/fee pairs)'
FROM (SELECT sum(l.amount) - max(l.amount) AS extra
      FROM student_ledger_entries l JOIN s ON s.id = l.school_id JOIN y ON y.id = l.academic_year_id
      WHERE l.transaction_type = 'FEE_ASSINMENT'
      GROUP BY l.student_id, l.reference_id HAVING count(*) > 1) d
UNION ALL
-- School fees on students' accounts this year, by the level of the class - an accountant limited to some sections
-- only counts those levels.
SELECT '8. fees on accounts by level: ' || c.level,
       to_char(sum(f.amount), 'FM999,999,990.00') || '  (' || count(DISTINCT sf.student_id) || ' students)'
FROM student_fees sf JOIN s ON s.id = sf.school_id
JOIN fee_structures f ON f.id = sf.fee_id AND NOT f.system JOIN y ON y.id = f.academic_year_id
JOIN enrollments e ON e.id = sf.enrollment_id JOIN classes c ON c.id = e.class_id
GROUP BY c.level
UNION ALL
-- Accountants limited to some sections only see those sections' figures.
SELECT '5. accountant sections: ' || u.email,
       coalesce((SELECT string_agg(ms.name || ' (' || coalesce((SELECT string_agg(sl.level, '/') FROM school_levels sl
                                                     WHERE sl.management_section_id = ms.id), 'no levels') || ')', ', ')
                 FROM staff_management_sections sms
                 JOIN management_sections ms ON ms.id = sms.management_section_id
                 WHERE sms.user_id = su.user_id AND sms.school_id = su.school_id), 'whole school')
FROM school_users su JOIN s ON s.id = su.school_id JOIN users u ON u.id = su.user_id
WHERE su.role = 'ACCOUNTANT'
ORDER BY 1;
