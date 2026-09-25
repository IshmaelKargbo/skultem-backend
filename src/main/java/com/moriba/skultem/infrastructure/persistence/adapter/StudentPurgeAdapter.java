package com.moriba.skultem.infrastructure.persistence.adapter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.repository.StudentPurgeRepository;

import lombok.RequiredArgsConstructor;

// Plain SQL on purpose: none of the student's dependents cascade (every foreign key is NO ACTION), so
// each table is emptied leaf-first, inside the caller's transaction - all of it or none of it.
@Repository
@RequiredArgsConstructor
public class StudentPurgeAdapter implements StudentPurgeRepository {

    private static final String ENROLLMENTS = "(SELECT id FROM enrollments WHERE student_id = ? AND school_id = ?)";

    private final JdbcTemplate jdbc;

    @Override
    public boolean hasRecordedPayments(String schoolId, String studentId) {
        Integer payments = jdbc.queryForObject(
                "SELECT count(*) FROM payments WHERE student_id = ? AND school_id = ?", Integer.class, studentId,
                schoolId);
        Integer sales = jdbc.queryForObject(
                "SELECT count(*) FROM material_sales WHERE student_id = ? AND school_id = ? AND amount_paid > 0",
                Integer.class, studentId, schoolId);
        return (payments != null && payments > 0) || (sales != null && sales > 0);
    }

    @Override
    public Purged purge(String schoolId, String studentId) {
        // Grading: scores -> the student's assessment rows.
        jdbc.update("DELETE FROM assessment_scores WHERE student_assessment_id IN "
                + "(SELECT id FROM student_assessments WHERE enrollment_id IN " + ENROLLMENTS + ")", studentId,
                schoolId);
        int assessments = jdbc.update("DELETE FROM student_assessments WHERE enrollment_id IN " + ENROLLMENTS,
                studentId, schoolId);

        // Things recorded against the enrollment.
        jdbc.update("DELETE FROM attendance WHERE enrollment_id IN " + ENROLLMENTS, studentId, schoolId);
        jdbc.update("DELETE FROM behaviours WHERE enrollment_id IN " + ENROLLMENTS, studentId, schoolId);
        jdbc.update("DELETE FROM fee_discounts WHERE enrollment_id IN " + ENROLLMENTS, studentId, schoolId);
        jdbc.update("DELETE FROM promotion_request_items WHERE student_id = ?", studentId);
        jdbc.update("DELETE FROM enrollment_subjcts WHERE student_id = ?", studentId);

        // Fees: the ledger and what the student was charged.
        jdbc.update("DELETE FROM student_ledger_entries WHERE student_id = ? AND school_id = ?", studentId, schoolId);
        int fees = jdbc.update("DELETE FROM student_fees WHERE student_id = ? AND school_id = ?", studentId,
                schoolId);

        // Material sales and supplies point at each other - break the loop, then delete both.
        jdbc.update("UPDATE material_sales SET supply_id = NULL WHERE student_id = ? AND school_id = ?", studentId,
                schoolId);
        jdbc.update("DELETE FROM supplies WHERE student_id = ? AND school_id = ?", studentId, schoolId);
        jdbc.update("DELETE FROM material_sales WHERE student_id = ? AND school_id = ?", studentId, schoolId);

        jdbc.update("DELETE FROM report_cards WHERE student_id = ? AND school_id = ?", studentId, schoolId);
        jdbc.update("DELETE FROM student_parents WHERE student_id = ?", studentId);
        int enrollments = jdbc.update("DELETE FROM enrollments WHERE student_id = ? AND school_id = ?", studentId,
                schoolId);
        jdbc.update("DELETE FROM students WHERE id = ? AND school_id = ?", studentId, schoolId);

        return new Purged(enrollments, fees, assessments);
    }
}
