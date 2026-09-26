package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.repository.ClassPurgeRepository;

// Plain SQL on purpose: none of a class's dependents cascade (every foreign key is NO ACTION), so each
// table is emptied leaf-first, inside the caller's transaction - all of it or none of it.
@Repository
public class ClassPurgeAdapter implements ClassPurgeRepository {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public ClassPurgeAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.named = new NamedParameterJdbcTemplate(jdbc);
    }

    @Override
    public int recordsInClass(String schoolId, String classId) {
        return count("SELECT count(*) FROM enrollments WHERE class_id = ? AND school_id = ?", classId, schoolId)
                + count("SELECT count(*) FROM students WHERE session_id IN "
                        + "(SELECT id FROM class_sessions WHERE class_id = ? AND school_id = ?)", classId, schoolId)
                + count("SELECT count(*) FROM report_cards WHERE class_id = ? AND school_id = ?", classId, schoolId);
    }

    @Override
    public boolean hasBilledFees(String schoolId, String classId) {
        String fees = "(SELECT id FROM fee_structures WHERE class_id = ? AND school_id = ?)";
        return count("SELECT count(*) FROM student_fees WHERE fee_id IN " + fees, classId, schoolId) > 0
                || count("SELECT count(*) FROM payments WHERE fee_id IN " + fees, classId, schoolId) > 0
                || count("SELECT count(*) FROM fee_discounts WHERE fee_id IN " + fees, classId, schoolId) > 0;
    }

    @Override
    public void purgeClass(String schoolId, String classId) {
        List<String> sessions = jdbc.queryForList(
                "SELECT id FROM class_sessions WHERE class_id = ? AND school_id = ?", String.class, classId, schoolId);
        purgeSessions(sessions);

        // Class-level setup.
        jdbc.update("DELETE FROM fee_structure_supply_items WHERE fee_structure_id IN "
                + "(SELECT id FROM fee_structures WHERE class_id = ? AND school_id = ?)", classId, schoolId);
        jdbc.update("DELETE FROM fee_structures WHERE class_id = ? AND school_id = ?", classId, schoolId);

        jdbc.update("DELETE FROM class_subjects WHERE class_id = ?", classId);
        jdbc.update("DELETE FROM class_subjects WHERE subject_group_id IN "
                + "(SELECT id FROM subject_groups WHERE class_id = ?)", classId);
        jdbc.update("DELETE FROM stream_subjects WHERE subject_group_id IN "
                + "(SELECT id FROM subject_groups WHERE class_id = ?)", classId);
        jdbc.update("DELETE FROM subject_groups WHERE class_id = ?", classId);

        jdbc.update("DELETE FROM class_streams WHERE class_id = ?", classId);
        jdbc.update("DELETE FROM class_sections WHERE class_id = ?", classId);

        // Another class's "promotes to" pointing here just becomes "none".
        jdbc.update("UPDATE classes SET next_class = NULL WHERE next_class = ?", classId);
        jdbc.update("DELETE FROM classes WHERE id = ? AND school_id = ?", classId, schoolId);
    }

    @Override
    public int studentsInSession(String schoolId, String sessionId) {
        return count("SELECT count(*) FROM students WHERE session_id = ? AND school_id = ?", sessionId, schoolId)
                + count("SELECT count(*) FROM enrollments e JOIN class_sessions s "
                        + "ON s.class_id = e.class_id AND s.academic_year_id = e.academic_year_id "
                        + "AND s.section_id = e.section_id AND (s.stream_id = e.stream_id OR (s.stream_id IS NULL AND e.stream_id IS NULL)) "
                        + "WHERE s.id = ? AND s.school_id = ?", sessionId, schoolId);
    }

    @Override
    public void purgeSession(String schoolId, String sessionId) {
        purgeSessions(jdbc.queryForList("SELECT id FROM class_sessions WHERE id = ? AND school_id = ?", String.class,
                sessionId, schoolId));
    }

    // Everything recorded against these sessions, leaf-first, then the sessions themselves.
    private void purgeSessions(List<String> sessionIds) {
        if (sessionIds.isEmpty()) {
            return;
        }
        var p = Map.of("ids", sessionIds);

        String masters = "(SELECT id FROM class_masters WHERE class_session_id IN (:ids))";
        String subjects = "(SELECT id FROM teacher_subjects WHERE class_session_id IN (:ids))";
        String cycles = "(SELECT id FROM class_subject_assessment_life_cycle WHERE teacher_subject_id IN " + subjects + ")";
        String requests = "(SELECT id FROM promotion_requests WHERE class_session_id IN (:ids) OR class_master_id IN " + masters + ")";
        String schemes = "(SELECT id FROM scheme_of_works WHERE session_id IN (:ids))";

        named.update("DELETE FROM assessment_approval_request WHERE class_master_id IN " + masters
                + " OR teacher_subject_id IN " + subjects
                + " OR class_subject_assessment_life_cycle_id IN " + cycles, p);
        named.update("DELETE FROM assessment_scores WHERE class_subject_assessment_life_cycle_id IN " + cycles
                + " OR student_assessment_id IN (SELECT id FROM student_assessments WHERE teacher_subject_id IN " + subjects + ")", p);
        named.update("DELETE FROM student_assessments WHERE teacher_subject_id IN " + subjects, p);
        named.update("DELETE FROM class_subject_assessment_life_cycle WHERE teacher_subject_id IN " + subjects, p);

        named.update("DELETE FROM timetables WHERE teacher_subject_id IN " + subjects
                + " OR period_id IN (SELECT id FROM periods WHERE session_id IN (:ids))", p);
        named.update("DELETE FROM periods WHERE session_id IN (:ids)", p);

        named.update("DELETE FROM promotion_request_items WHERE promotion_request_id IN " + requests, p);
        named.update("DELETE FROM promotion_requests WHERE class_session_id IN (:ids) OR class_master_id IN " + masters, p);

        named.update("DELETE FROM lessons WHERE week_id IN (SELECT id FROM weeks WHERE scheme_id IN " + schemes + ")", p);
        named.update("DELETE FROM weeks WHERE scheme_id IN " + schemes, p);
        named.update("DELETE FROM scheme_of_works WHERE session_id IN (:ids)", p);

        named.update("DELETE FROM class_masters WHERE class_session_id IN (:ids)", p);
        named.update("DELETE FROM teacher_subjects WHERE class_session_id IN (:ids)", p);
        named.update("DELETE FROM class_sessions WHERE id IN (:ids)", p);
    }

    private int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }
}
