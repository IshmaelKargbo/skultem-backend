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

    @Override
    public List<WrongGroupCharge> wrongGroupCharges(String schoolId) {
        return jdbc.query("""
                SELECT sf.student_id, concat_ws(' ', s.given_names, s.family_name) AS student_name,
                       s.admission_number, s.enrollment_type, c.name AS class_name, f.id AS fee_id,
                       cat.name AS fee_name, t.name AS term_name, f.new_students_only, f.amount,
                       COALESCE((SELECT sum(p.amount) FROM payments p
                                 WHERE p.fee_id = sf.fee_id AND p.student_id = sf.student_id
                                   AND p.school_id = sf.school_id), 0) AS paid,
                       (sf.discount_id IS NOT NULL OR EXISTS (SELECT 1 FROM fee_discounts d
                            WHERE d.fee_id = sf.fee_id AND d.enrollment_id = sf.enrollment_id
                              AND d.school_id = sf.school_id)) AS discounted
                FROM student_fees sf
                JOIN fee_structures f ON f.id = sf.fee_id
                JOIN students s ON s.id = sf.student_id
                JOIN enrollments e ON e.id = sf.enrollment_id
                JOIN classes c ON c.id = e.class_id
                JOIN fee_categories cat ON cat.id = f.category_id
                JOIN terms t ON t.id = f.term_id
                JOIN academic_years y ON y.id = f.academic_year_id
                WHERE sf.school_id = ? AND y.status = 'OPENED'
                  AND ((f.new_students_only AND s.enrollment_type NOT IN ('NEW', 'TRANSFER'))
                    OR (f.old_students_only AND s.enrollment_type IN ('NEW', 'TRANSFER')))
                ORDER BY c.name, student_name, t.name, cat.name
                """, (rs, i) -> new WrongGroupCharge(rs.getString("student_id"), rs.getString("student_name"),
                rs.getString("admission_number"), rs.getString("enrollment_type"), rs.getString("class_name"),
                rs.getString("fee_id"), rs.getString("fee_name"), rs.getString("term_name"),
                rs.getBoolean("new_students_only"), rs.getBigDecimal("amount"),
                rs.getBigDecimal("paid") == null ? BigDecimal.ZERO : rs.getBigDecimal("paid"),
                rs.getBoolean("discounted")), schoolId);
    }
}
