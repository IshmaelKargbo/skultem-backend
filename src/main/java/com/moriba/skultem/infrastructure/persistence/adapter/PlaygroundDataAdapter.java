package com.moriba.skultem.infrastructure.persistence.adapter;

import static com.moriba.skultem.domain.model.PlaygroundDataCategory.ASSESSMENTS;
import static com.moriba.skultem.domain.model.PlaygroundDataCategory.ATTENDANCE;
import static com.moriba.skultem.domain.model.PlaygroundDataCategory.BEHAVIOUR;
import static com.moriba.skultem.domain.model.PlaygroundDataCategory.CLASS_SETUP;
import static com.moriba.skultem.domain.model.PlaygroundDataCategory.COMMUNICATION;
import static com.moriba.skultem.domain.model.PlaygroundDataCategory.EXPENSES;
import static com.moriba.skultem.domain.model.PlaygroundDataCategory.FEES;
import static com.moriba.skultem.domain.model.PlaygroundDataCategory.MATERIALS;
import static com.moriba.skultem.domain.model.PlaygroundDataCategory.NOTIFICATIONS;
import static com.moriba.skultem.domain.model.PlaygroundDataCategory.PAYROLL;
import static com.moriba.skultem.domain.model.PlaygroundDataCategory.STAFF_ATTENDANCE;
import static com.moriba.skultem.domain.model.PlaygroundDataCategory.STUDENTS;
import static com.moriba.skultem.domain.model.PlaygroundDataCategory.SUBJECT_SETUP;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.PlaygroundDataCategory;
import com.moriba.skultem.domain.repository.PlaygroundDataRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Native SQL on purpose: a category spans several tables whose foreign keys dictate the delete
 * order (see the comments in {@link #wipe}), and bulk statements scoped by school_id are the only
 * way to do that without loading every row into memory. Every statement is bound to :schoolId -
 * nothing here can reach another tenant's rows.
 */
@Repository
public class PlaygroundDataAdapter implements PlaygroundDataRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Map<PlaygroundDataCategory, Map<String, Long>> countByCategory(String schoolId) {
        var result = new EnumMap<PlaygroundDataCategory, Map<String, Long>>(PlaygroundDataCategory.class);

        result.put(STUDENTS, counts(schoolId,
                "Students", "SELECT count(*) FROM students WHERE school_id = :schoolId",
                "Guardians", "SELECT count(*) FROM parents WHERE school_id = :schoolId",
                "Enrollments", "SELECT count(*) FROM enrollments WHERE school_id = :schoolId",
                "Promotion requests", "SELECT count(*) FROM promotion_requests WHERE school_id = :schoolId"));
        result.put(FEES, counts(schoolId,
                "Payments", "SELECT count(*) FROM payments WHERE school_id = :schoolId",
                "Discounts", "SELECT count(*) FROM fee_discounts WHERE school_id = :schoolId"));
        result.put(ASSESSMENTS, counts(schoolId,
                "Marks entered", "SELECT count(*) FROM assessment_scores WHERE school_id = :schoolId"
                        + " AND (score <> 0 OR graded_by_user_id IS NOT NULL)",
                "Report cards", "SELECT count(*) FROM report_cards WHERE school_id = :schoolId",
                "Grade-sheet approvals",
                "SELECT count(*) FROM assessment_approval_request WHERE school_id = :schoolId"));
        result.put(ATTENDANCE, counts(schoolId,
                "Attendance records", "SELECT count(*) FROM attendance WHERE school_id = :schoolId"));
        result.put(BEHAVIOUR, counts(schoolId,
                "Behaviour records", "SELECT count(*) FROM behaviours WHERE school_id = :schoolId"));
        result.put(MATERIALS, counts(schoolId,
                "Sales", "SELECT count(*) FROM material_sales WHERE school_id = :schoolId",
                "Issues", "SELECT count(*) FROM supplies WHERE school_id = :schoolId"));
        result.put(EXPENSES, counts(schoolId,
                "Expenses", "SELECT count(*) FROM expenses WHERE school_id = :schoolId"));
        result.put(PAYROLL, counts(schoolId,
                "Payroll runs", "SELECT count(*) FROM payroll_runs WHERE school_id = :schoolId",
                "Payslips", "SELECT count(*) FROM payslips WHERE school_id = :schoolId"));
        result.put(STAFF_ATTENDANCE, counts(schoolId,
                "Clock-ins", "SELECT count(*) FROM teacher_attendances WHERE school_id = :schoolId",
                "Leave requests", "SELECT count(*) FROM leave_requests WHERE school_id = :schoolId"));
        result.put(COMMUNICATION, counts(schoolId,
                "Notices", "SELECT count(*) FROM notices WHERE school_id = :schoolId",
                "Broadcasts", "SELECT count(*) FROM broadcasts WHERE school_id = :schoolId",
                "Calendar events", "SELECT count(*) FROM calendar_events WHERE school_id = :schoolId"));
        result.put(SUBJECT_SETUP, counts(schoolId,
                "Class subjects", "SELECT (SELECT count(*) FROM class_subjects WHERE school_id = :schoolId)"
                        + " + (SELECT count(*) FROM stream_subjects WHERE school_id = :schoolId)",
                "Teacher assignments", "SELECT count(*) FROM teacher_subjects WHERE school_id = :schoolId",
                "Subject groups", "SELECT count(*) FROM subject_groups WHERE school_id = :schoolId",
                "Timetable entries", "SELECT count(*) FROM timetables WHERE school_id = :schoolId"));
        result.put(CLASS_SETUP, counts(schoolId,
                "Classes", "SELECT count(*) FROM classes WHERE school_id = :schoolId",
                "Sections", "SELECT count(*) FROM sections WHERE school_id = :schoolId",
                "Streams", "SELECT count(*) FROM streams WHERE school_id = :schoolId",
                "Class fee structures",
                "SELECT count(*) FROM fee_structures WHERE school_id = :schoolId AND class_id IS NOT NULL",
                "Schemes of work", "SELECT count(*) FROM scheme_of_works WHERE school_id = :schoolId"));
        result.put(NOTIFICATIONS, counts(schoolId,
                "Notifications", "SELECT count(*) FROM notifications WHERE school_id = :schoolId",
                "Activity feed", "SELECT count(*) FROM activities WHERE school_id = :schoolId"));

        return result;
    }

    @Override
    public void wipe(String schoolId, Set<PlaygroundDataCategory> categories) {
        // Push any pending entity changes out before bulk statements run underneath them, and drop
        // the (now stale) persistence context afterwards.
        em.flush();

        boolean studentsGo = categories.contains(STUDENTS);
        // Grade-sheet rows hang off teacher assignments (teacher_subjects) as well as enrollments, so
        // they have to go outright - not just be reset - when either of those goes.
        boolean gradeSheetsGo = studentsGo || categories.contains(SUBJECT_SETUP);

        if (categories.contains(NOTIFICATIONS)) {
            exec(schoolId, "DELETE FROM notifications WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM activities WHERE school_id = :schoolId");
        }

        if (categories.contains(COMMUNICATION)) {
            exec(schoolId, "DELETE FROM notices WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM broadcasts WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM calendar_events WHERE school_id = :schoolId");
        }

        if (categories.contains(STAFF_ATTENDANCE)) {
            exec(schoolId, "DELETE FROM teacher_attendances WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM leave_requests WHERE school_id = :schoolId");
        }

        if (categories.contains(PAYROLL)) {
            exec(schoolId, "DELETE FROM payslips WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM payroll_runs WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM transactions WHERE school_id = :schoolId AND reference_type = 'PAYROLL'");
        }

        if (categories.contains(EXPENSES)) {
            exec(schoolId, "DELETE FROM expenses WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM transactions WHERE school_id = :schoolId AND reference_type = 'EXPENSE'");
        }

        if (categories.contains(BEHAVIOUR)) {
            exec(schoolId, "DELETE FROM behaviours WHERE school_id = :schoolId");
        }

        if (categories.contains(ATTENDANCE)) {
            exec(schoolId, "DELETE FROM attendance WHERE school_id = :schoolId");
        }

        if (categories.contains(ASSESSMENTS)) {
            exec(schoolId, "DELETE FROM report_cards WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM assessment_approval_request WHERE school_id = :schoolId");

            if (gradeSheetsGo) {
                // Nobody (or no teacher assignment) left to hold marks - drop the per-student rows and
                // the grade-sheet cycles too. ProvisionStudentAssessmentsUseCase recreates them once
                // students are enrolled and subjects assigned again.
                exec(schoolId, "DELETE FROM assessment_scores WHERE school_id = :schoolId");
                exec(schoolId, "DELETE FROM student_assessments WHERE school_id = :schoolId");
                exec(schoolId, "DELETE FROM class_subject_assessment_life_cycle WHERE school_id = :schoolId");
            } else {
                // Students stay, so their score rows must too - those are only ever created on
                // enrollment, and deleting them would leave kept students with no grade sheet.
                // Blank the marks and put every cycle back where provisioning starts it.
                exec(schoolId, "UPDATE assessment_scores SET score = 0, graded_by_user_id = NULL,"
                        + " updated_at = now() WHERE school_id = :schoolId");
                exec(schoolId, """
                        UPDATE class_subject_assessment_life_cycle c
                        SET status = CASE WHEN t.term_number = 1 AND a.position = 1 THEN 'DRAFT' ELSE 'LOCKED' END,
                            updated_at = now()
                        FROM terms t, assessments a
                        WHERE c.term_id = t.id AND c.assessment_id = a.id AND c.school_id = :schoolId
                        """);
            }
        }

        if (categories.contains(MATERIALS)) {
            // Every OUT movement is a supply being collected (see SupplyMaterialUseCase), so adding
            // them back undoes exactly what testing took off the shelf. Restocks (IN) are real
            // inventory and stay.
            exec(schoolId, """
                    UPDATE materials m
                    SET stock_quantity = m.stock_quantity + o.qty, updated_at = now()
                    FROM (SELECT material_id, sum(qty) AS qty FROM material_transactions
                          WHERE school_id = :schoolId AND direction = 'OUT' GROUP BY material_id) o
                    WHERE m.id = o.material_id AND m.school_id = :schoolId
                    """);
            exec(schoolId, "DELETE FROM material_transactions WHERE school_id = :schoolId AND direction = 'OUT'");
            // supplies <-> material_sales reference each other; both FKs are deferred to commit (V44).
            exec(schoolId, "DELETE FROM material_sales WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM supplies WHERE school_id = :schoolId");
            exec(schoolId,
                    "DELETE FROM transactions WHERE school_id = :schoolId AND reference_type = 'MATERIAL_SALE'");
        }

        if (categories.contains(FEES)) {
            exec(schoolId, "DELETE FROM payments WHERE school_id = :schoolId");
            exec(schoolId, "UPDATE student_fees SET discount_id = NULL WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM fee_discounts WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM transactions WHERE school_id = :schoolId AND reference_type = 'STUDENT'");

            if (studentsGo) {
                exec(schoolId, "DELETE FROM student_ledger_entries WHERE school_id = :schoolId");
            } else {
                // Kept students keep what they're charged - only payments/discounts/refunds go, and
                // each student's running balance is rebuilt from the charges left behind (same
                // ordering CreateStudentLedgerUsercase reads the latest entry by).
                exec(schoolId, "DELETE FROM student_ledger_entries WHERE school_id = :schoolId"
                        + " AND transaction_type <> 'FEE_ASSINMENT'");
                exec(schoolId, """
                        UPDATE student_ledger_entries e
                        SET balance = r.balance
                        FROM (SELECT id, sum(CASE WHEN direction = 'DEBIT' THEN amount ELSE -amount END)
                                  OVER (PARTITION BY student_id ORDER BY paid_at, created_at, id) AS balance
                              FROM student_ledger_entries WHERE school_id = :schoolId) r
                        WHERE e.id = r.id
                        """);
            }
        }

        if (studentsGo) {
            exec(schoolId, "DELETE FROM promotion_request_items WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM promotion_requests WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM student_fees WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM enrollment_subjcts WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM enrollments WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM student_parents WHERE school_id = :schoolId");

            // Guardians' logins: revoke their membership (and live sessions) at this school only.
            // The user account itself is global and may belong to other schools, so it stays.
            exec(schoolId, """
                    DELETE FROM user_sessions WHERE school_id = :schoolId
                      AND user_id IN (SELECT user_id FROM parents WHERE school_id = :schoolId AND user_id IS NOT NULL)
                    """);
            exec(schoolId, """
                    DELETE FROM school_users WHERE school_id = :schoolId AND role = 'PARENT'
                      AND user_id IN (SELECT user_id FROM parents WHERE school_id = :schoolId AND user_id IS NOT NULL)
                    """);

            // students.parent_id -> parents, so students first.
            exec(schoolId, "DELETE FROM students WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM parents WHERE school_id = :schoolId");
        }

        if (categories.contains(SUBJECT_SETUP)) {
            // Timetable entries point at teacher assignments; grade-sheet rows were cleared above.
            exec(schoolId, "DELETE FROM timetables WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM class_subjects WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM stream_subjects WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM subject_groups WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM teacher_subjects WHERE school_id = :schoolId");
        }

        if (categories.contains(CLASS_SETUP)) {
            // Students, enrollments, fees, promotion requests and subject setup are already gone
            // (CLASS_SETUP requires them), so what's left under a class is its sessions and fee
            // structures, and what hangs off a session: curriculum, periods and class masters.
            exec(schoolId, "DELETE FROM lessons WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM weeks WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM scheme_of_works WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM periods WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM class_masters WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM class_sessions WHERE school_id = :schoolId");
            // Only fee structures bound to a class. School-wide ones (class_id NULL) - including the
            // platform fee BackfillPlatformFeesUseCase manages - aren't class setup and stay.
            exec(schoolId, """
                    DELETE FROM fee_structure_supply_items WHERE school_id = :schoolId AND fee_structure_id IN
                      (SELECT id FROM fee_structures WHERE school_id = :schoolId AND class_id IS NOT NULL)
                    """);
            exec(schoolId, "DELETE FROM fee_structures WHERE school_id = :schoolId AND class_id IS NOT NULL");
            exec(schoolId, "DELETE FROM class_sections WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM class_streams WHERE school_id = :schoolId");
            // classes.next_class points at another class of the same school.
            exec(schoolId, "UPDATE classes SET next_class = NULL WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM classes WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM sections WHERE school_id = :schoolId");
            exec(schoolId, "DELETE FROM streams WHERE school_id = :schoolId");
        }

        if (categories.contains(FEES) || categories.contains(EXPENSES) || categories.contains(PAYROLL)
                || categories.contains(MATERIALS)) {
            // transactions.balance is a running total (see CreateTransactionUsercase, which builds
            // on the latest row by created_at). Rows left behind still carry totals that included
            // what was just removed, so rebuild them in the same order.
            exec(schoolId, """
                    UPDATE transactions t
                    SET balance = r.balance
                    FROM (SELECT id, sum(CASE WHEN direction = 'CREDIT' THEN amount ELSE -amount END)
                              OVER (ORDER BY created_at, id) AS balance
                          FROM transactions WHERE school_id = :schoolId) r
                    WHERE t.id = r.id
                    """);
        }

        em.clear();
    }

    private Map<String, Long> counts(String schoolId, String... labelAndSql) {
        var result = new LinkedHashMap<String, Long>();
        for (int i = 0; i < labelAndSql.length; i += 2) {
            var value = (Number) em.createNativeQuery(labelAndSql[i + 1])
                    .setParameter("schoolId", schoolId)
                    .getSingleResult();
            result.put(labelAndSql[i], value.longValue());
        }
        return result;
    }

    private void exec(String schoolId, String sql) {
        em.createNativeQuery(sql).setParameter("schoolId", schoolId).executeUpdate();
    }
}
