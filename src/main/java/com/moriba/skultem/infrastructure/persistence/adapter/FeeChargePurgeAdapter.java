package com.moriba.skultem.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.repository.FeeChargePurgeRepository;

import lombok.RequiredArgsConstructor;

// Plain SQL, inside the caller's transaction - all of it or none of it. Ledger entries point at the fee
// (charges) or at the discount (discount credits) through reference_id.
@Repository
@RequiredArgsConstructor
public class FeeChargePurgeAdapter implements FeeChargePurgeRepository {

    private final JdbcTemplate jdbc;

    @Override
    public long countPayments(String schoolId, String feeId) {
        Long n = jdbc.queryForObject("SELECT count(*) FROM payments WHERE fee_id = ? AND school_id = ?", Long.class,
                feeId, schoolId);
        return n == null ? 0 : n;
    }

    @Override
    public List<Charge> charges(String schoolId, String feeId) {
        return jdbc.query("""
                SELECT sf.student_id, sf.enrollment_id,
                       COALESCE((SELECT sum(p.amount) FROM payments p
                                 WHERE p.fee_id = sf.fee_id AND p.student_id = sf.student_id
                                   AND p.school_id = sf.school_id), 0) AS paid
                FROM student_fees sf WHERE sf.fee_id = ? AND sf.school_id = ?
                """, (rs, i) -> new Charge(rs.getString("student_id"), rs.getString("enrollment_id"),
                rs.getBigDecimal("paid") == null ? BigDecimal.ZERO : rs.getBigDecimal("paid")), feeId, schoolId);
    }

    @Override
    public List<String> removeCharges(String schoolId, String feeId) {
        List<String> students = jdbc.queryForList(
                "SELECT DISTINCT student_id FROM student_fees WHERE fee_id = ? AND school_id = ?", String.class,
                feeId, schoolId);

        jdbc.update("DELETE FROM student_ledger_entries WHERE school_id = ? AND transaction_type = 'DISCOUNT' "
                + "AND reference_id IN (SELECT id FROM fee_discounts WHERE fee_id = ? AND school_id = ?)",
                schoolId, feeId, schoolId);
        jdbc.update("DELETE FROM student_ledger_entries WHERE school_id = ? AND transaction_type = 'FEE_ASSINMENT' "
                + "AND reference_id = ?", schoolId, feeId);
        jdbc.update("DELETE FROM student_fees WHERE fee_id = ? AND school_id = ?", feeId, schoolId);
        jdbc.update("DELETE FROM fee_discounts WHERE fee_id = ? AND school_id = ?", feeId, schoolId);

        return students;
    }

    @Override
    public void removeCharges(String schoolId, String feeId, List<String> studentIds) {
        for (String studentId : studentIds) {
            jdbc.update("DELETE FROM student_ledger_entries WHERE school_id = ? AND student_id = ? "
                    + "AND transaction_type = 'DISCOUNT' AND reference_id IN "
                    + "(SELECT d.id FROM fee_discounts d JOIN enrollments e ON e.id = d.enrollment_id "
                    + " WHERE d.fee_id = ? AND d.school_id = ? AND e.student_id = ?)",
                    schoolId, studentId, feeId, schoolId, studentId);
            jdbc.update("DELETE FROM student_ledger_entries WHERE school_id = ? AND student_id = ? "
                    + "AND transaction_type = 'FEE_ASSINMENT' AND reference_id = ?", schoolId, studentId, feeId);
            jdbc.update("DELETE FROM student_fees WHERE fee_id = ? AND school_id = ? AND student_id = ?", feeId,
                    schoolId, studentId);
            jdbc.update("DELETE FROM fee_discounts WHERE fee_id = ? AND school_id = ? AND enrollment_id IN "
                    + "(SELECT id FROM enrollments WHERE student_id = ? AND school_id = ?)",
                    feeId, schoolId, studentId, schoolId);
        }
    }
}
