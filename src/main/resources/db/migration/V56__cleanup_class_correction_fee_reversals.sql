-- The first version of "Change Class" (ChangeEnrollmentClassUseCase) removed a student's old fee
-- charges by posting an ADJUSTMENT credit titled "Reversal of <fee> (<term>) - class corrected for
-- <student>" next to the original FEE_ASSINMENT debit, so the old fee stayed in the ledger and the
-- credit read as money paid. It now deletes the old debit outright instead. This removes what that
-- first version left behind: each such credit, plus the one old debit it was offsetting, so the
-- ledger only shows the new class's fees. Nothing here touches real payments or the cashbook.
--
-- Safe to re-run: once the reversal entries are gone there is nothing left to match.
DO $$
DECLARE
    affected text[];
BEGIN
    SELECT array_agg(DISTINCT student_id) INTO affected
    FROM student_ledger_entries
    WHERE transaction_type = 'ADJUSTMENT'
      AND direction = 'CREDIT'
      AND description LIKE 'Reversal of % - class corrected for %';

    IF affected IS NULL THEN
        RETURN;
    END IF;

    -- One reversal offsets exactly one old charge of the same fee for the same student, so for each
    -- (student, fee) pair drop as many of the oldest charges as there are reversals. (A student
    -- moved away from a class and back again keeps the newer charge, which is the live one.)
    WITH reversals AS (
        SELECT id, student_id, reference_id
        FROM student_ledger_entries
        WHERE transaction_type = 'ADJUSTMENT'
          AND direction = 'CREDIT'
          AND description LIKE 'Reversal of % - class corrected for %'
    ),
    reversal_counts AS (
        SELECT student_id, reference_id, count(*) AS reversal_count
        FROM reversals
        GROUP BY student_id, reference_id
    ),
    ranked_charges AS (
        SELECT e.id,
               c.reversal_count,
               row_number() OVER (PARTITION BY e.student_id, e.reference_id ORDER BY e.paid_at, e.created_at) AS position
        FROM student_ledger_entries e
        JOIN reversal_counts c ON c.student_id = e.student_id AND c.reference_id = e.reference_id
        WHERE e.transaction_type = 'FEE_ASSINMENT'
    )
    DELETE FROM student_ledger_entries
    WHERE id IN (SELECT id FROM reversals)
       OR id IN (SELECT id FROM ranked_charges WHERE position <= reversal_count);

    -- Every later entry's stored running balance depended on the rows just removed, so rebuild them
    -- the same way RecomputeStudentLedgerBalancesUseCase does: oldest first, debit up, credit down.
    WITH running AS (
        SELECT id,
               sum(CASE direction WHEN 'DEBIT' THEN amount ELSE -amount END)
                   OVER (PARTITION BY student_id ORDER BY paid_at, created_at, id ROWS UNBOUNDED PRECEDING) AS balance
        FROM student_ledger_entries
        WHERE student_id = ANY (affected)
    )
    UPDATE student_ledger_entries e
    SET balance = r.balance
    FROM running r
    WHERE e.id = r.id
      AND e.balance IS DISTINCT FROM r.balance;
END $$;
