package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.repository.ParentPurgeRepository.AccountOutcome;
import com.moriba.skultem.domain.repository.TeacherPurgeRepository;

import lombok.RequiredArgsConstructor;

// Plain SQL, inside the caller's transaction - all of it or none of it.
@Repository
@RequiredArgsConstructor
public class TeacherPurgeAdapter implements TeacherPurgeRepository {

    private final JdbcTemplate jdbc;

    // Every table that points at a teacher and is a record worth keeping (exam invigilator slots are a schedule,
    // removed with the teacher). A new table that points at teachers belongs here too.
    private static final Map<String, String> HISTORY = new LinkedHashMap<>();
    static {
        HISTORY.put("subject assignment(s)", "SELECT count(*) FROM teacher_subjects WHERE teacher_id = ?");
        HISTORY.put("class master role(s)", "SELECT count(*) FROM class_masters WHERE teacher_id = ?");
        HISTORY.put("house master role(s)", "SELECT count(*) FROM houses_house_masters WHERE house_masters_id = ?");
        HISTORY.put("salary structure(s)", "SELECT count(*) FROM salary_structures WHERE teacher_id = ?");
        HISTORY.put("payslip(s)", "SELECT count(*) FROM payslips WHERE teacher_id = ?");
        HISTORY.put("leave request(s)", "SELECT count(*) FROM leave_requests WHERE teacher_id = ?");
        HISTORY.put("attendance record(s)", "SELECT count(*) FROM teacher_attendances WHERE teacher_id = ?");
        HISTORY.put("library loan(s)", "SELECT count(*) FROM library_loans WHERE teacher_id = ?");
    }

    @Override
    public Map<String, Long> history(String teacherId) {
        Map<String, Long> out = new LinkedHashMap<>();
        HISTORY.forEach((label, sql) -> {
            long n = count(sql, teacherId);
            if (n > 0) {
                out.put(label, n);
            }
        });
        return out;
    }

    @Override
    public AccountOutcome purge(String schoolId, String teacherId) {
        String userId = jdbc.queryForObject("SELECT user_id FROM teachers WHERE id = ? AND school_id = ?",
                String.class, teacherId, schoolId);

        jdbc.update("DELETE FROM exam_paper_invigilators WHERE teacher_id = ?", teacherId);
        jdbc.update("DELETE FROM teachers WHERE id = ? AND school_id = ?", teacherId, schoolId);
        jdbc.update("DELETE FROM staff_management_sections WHERE school_id = ? AND user_id = ?", schoolId, userId);
        // Their staff roles here go; a parent role (their own children at the school) stays.
        jdbc.update("DELETE FROM school_users WHERE school_id = ? AND user_id = ? AND role <> 'PARENT'", schoolId,
                userId);

        // The login account: only touched when this was its last use (same rules as a deleted parent).
        if (count("SELECT count(*) FROM school_users WHERE user_id = ?", userId) > 0
                || count("SELECT count(*) FROM parents WHERE user_id = ?", userId) > 0
                || count("SELECT count(*) FROM teachers WHERE user_id = ?", userId) > 0) {
            return AccountOutcome.KEPT;
        }

        jdbc.update("DELETE FROM user_sessions WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM notifications WHERE user_id = ?", userId);

        // Audit entries, notices and the like still point at accounts that did things - keep those (the record of
        // who did what must survive) but disable the account and free its email.
        boolean referenced = count("SELECT count(*) FROM audit_logs WHERE user_id = ?", userId) > 0
                || count("SELECT count(*) FROM notices WHERE posted_by_user_id = ?", userId) > 0
                || count("SELECT count(*) FROM calendar_events WHERE created_by_user_id = ?", userId) > 0
                || count("SELECT count(*) FROM broadcasts WHERE sent_by_user_id = ?", userId) > 0;
        if (referenced) {
            jdbc.update("UPDATE users SET status = 'DELETED', email = NULL WHERE id = ?", userId);
            return AccountOutcome.DISABLED;
        }

        jdbc.update("DELETE FROM users WHERE id = ?", userId);
        return AccountOutcome.DELETED;
    }

    private long count(String sql, String id) {
        Long value = jdbc.queryForObject(sql, Long.class, id);
        return value == null ? 0 : value;
    }
}
