package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.HashSet;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.repository.CommunicationAudienceRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CommunicationAudienceAdapter implements CommunicationAudienceRepository {

    private final JdbcTemplate jdbc;

    // Students currently enrolled in a class whose level belongs to the section.
    private static final String SECTION_STUDENTS = """
            SELECT DISTINCT e.student_id
            FROM enrollments e
            JOIN classes c ON c.id = e.class_id
            JOIN school_levels sl ON sl.school_id = c.school_id AND sl.level = c.level
            WHERE e.school_id = ? AND e.status = 'ACTIVE' AND sl.management_section_id = ?
            """;

    @Override
    public Set<String> sectionIdsOfChildren(String schoolId, String parentUserId) {
        return new HashSet<>(jdbc.queryForList("""
                SELECT DISTINCT sl.management_section_id
                FROM parents p
                JOIN (SELECT id AS student_id, parent_id FROM students
                      UNION
                      SELECT student_id, parent_id FROM student_parents) kids ON kids.parent_id = p.id
                JOIN enrollments e ON e.student_id = kids.student_id AND e.status = 'ACTIVE'
                JOIN classes c ON c.id = e.class_id
                JOIN school_levels sl ON sl.school_id = c.school_id AND sl.level = c.level
                WHERE p.user_id = ? AND p.school_id = ? AND sl.management_section_id IS NOT NULL
                """, String.class, parentUserId, schoolId));
    }

    @Override
    public Counts countsForSection(String schoolId, String sectionId) {
        int students = count("SELECT count(*) FROM (" + SECTION_STUDENTS + ") s", schoolId, sectionId);

        int parents = count("""
                SELECT count(DISTINCT kids.parent_id)
                FROM (SELECT id AS student_id, parent_id FROM students
                      UNION
                      SELECT student_id, parent_id FROM student_parents) kids
                WHERE kids.student_id IN (""" + SECTION_STUDENTS + ")", schoolId, sectionId);

        // Teachers limited to this section, plus those with no limit (they work across every section).
        int teachers = count("""
                SELECT count(*) FROM teachers t
                WHERE t.school_id = ? AND t.status <> 'DELETED'
                  AND (EXISTS (SELECT 1 FROM staff_management_sections m WHERE m.school_id = t.school_id
                               AND m.user_id = t.user_id AND m.role = 'TEACHER' AND m.management_section_id = ?)
                       OR NOT EXISTS (SELECT 1 FROM staff_management_sections m WHERE m.school_id = t.school_id
                                      AND m.user_id = t.user_id AND m.role = 'TEACHER'))
                """, schoolId, sectionId);

        // Staff roles: owner-level are never limited; admin/accountant follow the same rule as teachers.
        int staff = count("""
                SELECT count(DISTINCT su.user_id) FROM school_users su
                WHERE su.school_id = ? AND su.status = 'ACTIVE'
                  AND su.role IN ('ADMIN', 'SUPER_ADMIN', 'ACCOUNTANT', 'PROPRIETOR', 'OWNER')
                  AND (su.role NOT IN ('ADMIN', 'ACCOUNTANT')
                       OR EXISTS (SELECT 1 FROM staff_management_sections m WHERE m.school_id = su.school_id
                                  AND m.user_id = su.user_id AND m.role = su.role AND m.management_section_id = ?)
                       OR NOT EXISTS (SELECT 1 FROM staff_management_sections m WHERE m.school_id = su.school_id
                                      AND m.user_id = su.user_id AND m.role = su.role))
                """, schoolId, sectionId);

        return new Counts(students, parents, teachers, staff);
    }

    private int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }
}
