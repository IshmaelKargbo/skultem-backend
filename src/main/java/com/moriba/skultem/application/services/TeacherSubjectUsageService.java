package com.moriba.skultem.application.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;

import lombok.RequiredArgsConstructor;

/**
 * Gets teacher assignments (teacher_subjects rows) ready to be deleted. The timetable,
 * assessments, assessment cycles and approval requests all point at them by foreign key, so a
 * plain delete fails at commit with a raw constraint error. Callers run these checks first: in-use
 * work (timetable slots, grades) is refused with a message, empty provisioned records are cleared.
 */
@Service
@RequiredArgsConstructor
public class TeacherSubjectUsageService {
    private final JdbcTemplate jdbc;

    /**
     * Gets one teacher assignment ready to be deleted (e.g. a teacher taken off a subject).
     * Refuses when it is on the timetable or holds grades / approval requests; otherwise deletes
     * the empty records provisioned for it so the caller's delete goes through.
     */
    public void prepareAssignmentRemoval(String teacherSubjectId, String teacherName, String subjectName) {
        Object[] a = { teacherSubjectId };
        String ids = "(?)";

        int slots = count("SELECT count(*) FROM timetables WHERE teacher_subject_id IN " + ids, a);
        if (slots > 0) {
            throw new RuleException("Cannot remove " + teacherName + " from " + subjectName + " - they have " + slots
                    + (slots == 1 ? " timetable slot" : " timetable slots")
                    + " for it. Give the subject another teacher instead, or update the timetable first.");
        }
        if (hasGrades(ids, a)) {
            throw new RuleException("Cannot remove " + teacherName + " from " + subjectName
                    + " - grades have already been recorded under this assignment. Give the subject another teacher instead.");
        }
        clearEmptyRecords(ids, a);
    }

    /**
     * Gets a subject's teacher assignments in a class (any session of it, narrowed to one stream
     * when given) ready to be deleted along with the subject. Refuses when they are on the
     * timetable or hold grades / approval requests - that is real work we won't silently drop.
     * Otherwise deletes the ungraded records hanging off them (provisioned student assessments
     * and assessment cycles) so the caller's delete of the assignments themselves can go through.
     */
    public void prepareSubjectRemoval(String schoolId, String classId, String streamId, String subjectId,
            String subjectName, String fromWhat) {
        var where = new StringBuilder("""
                SELECT ts.id FROM teacher_subjects ts
                JOIN class_sessions cs ON cs.id = ts.class_session_id
                WHERE ts.school_id = ? AND ts.subject_id = ?
                """);
        List<Object> args = new ArrayList<>(List.of(schoolId, subjectId));
        if (classId != null) {
            where.append(" AND cs.class_id = ?");
            args.add(classId);
        }
        if (streamId != null) {
            where.append(" AND cs.stream_id = ?");
            args.add(streamId);
        }
        String ids = "(" + where + ")";
        Object[] a = args.toArray();

        int slots = count("SELECT count(*) FROM timetables WHERE teacher_subject_id IN " + ids, a);
        if (slots > 0) {
            throw new RuleException("'" + subjectName + "' is on the timetable (" + slots
                    + (slots == 1 ? " slot" : " slots") + ") - remove it from the timetable before removing it from the "
                    + fromWhat);
        }

        if (hasGrades(ids, a)) {
            throw new RuleException("'" + subjectName + "' already has grades recorded in this " + fromWhat
                    + " - it can't be removed");
        }
        clearEmptyRecords(ids, a);
    }

    // Real grading under these assignments: a score entered (score > 0 or a grader recorded - the
    // same test the rest of the app uses; provisioning creates every score row at 0 up front), a
    // continuous-assessment entry, or a grade approval request.
    private boolean hasGrades(String ids, Object[] a) {
        String scores = scoreIds(ids);
        int graded = count("SELECT count(*) FROM assessment_scores WHERE id IN " + scores
                + " AND (score <> 0 OR graded_by_user_id IS NOT NULL)", concat(a, a));
        int caEntries = count("SELECT count(*) FROM assessment_ca_entries WHERE assessment_score_id IN " + scores,
                concat(a, a));
        int approvals = count("SELECT count(*) FROM assessment_approval_request WHERE teacher_subject_id IN " + ids, a);
        return graded > 0 || caEntries > 0 || approvals > 0;
    }

    // Nothing graded: the provisioned score rows, student assessments and assessment cycles are
    // empty shells. Deleted children-first so no foreign key is left pointing at a deleted row.
    private void clearEmptyRecords(String ids, Object[] a) {
        jdbc.update("DELETE FROM assessment_scores WHERE id IN " + scoreIds(ids), concat(a, a));
        jdbc.update("DELETE FROM student_assessments WHERE teacher_subject_id IN " + ids, a);
        jdbc.update("DELETE FROM class_subject_assessment_life_cycle WHERE teacher_subject_id IN " + ids, a);
    }

    // Score rows hang off both the student assessment and the cycle - either side ties them to
    // these assignments. Takes the assignment-id params twice.
    private static String scoreIds(String ids) {
        return """
                (SELECT sc.id FROM assessment_scores sc
                 WHERE sc.student_assessment_id IN (SELECT id FROM student_assessments WHERE teacher_subject_id IN %s)
                    OR sc.class_subject_assessment_life_cycle_id IN
                       (SELECT id FROM class_subject_assessment_life_cycle WHERE teacher_subject_id IN %s))
                """.formatted(ids, ids);
    }

    private int count(String sql, Object[] args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    private static Object[] concat(Object[] x, Object[] y) {
        Object[] out = new Object[x.length + y.length];
        System.arraycopy(x, 0, out, 0, x.length);
        System.arraycopy(y, 0, out, x.length, y.length);
        return out;
    }
}
