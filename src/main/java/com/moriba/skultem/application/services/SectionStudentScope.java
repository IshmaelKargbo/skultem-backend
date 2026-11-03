package com.moriba.skultem.application.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

// The students a section-limited staff member may see: those enrolled this academic year in a class at one of their
// sections' levels. For records that carry only a student id (sick bay visits, behaviour, attendance) and so can't be
// filtered by level directly. Whole-school callers (and non-scopable roles like the nurse) get null = no limit.
@Service
@RequiredArgsConstructor
public class SectionStudentScope {

    private final SectionScopeService sectionScopeService;
    private final JdbcTemplate jdbc;

    /** null when the caller sees the whole school; otherwise the student ids they may see (possibly empty). */
    public Set<String> studentIds(String schoolId) {
        Collection<com.moriba.skultem.domain.vo.Level> levels = sectionScopeService.restrictedLevels();
        if (levels == null) {
            return null;
        }
        if (levels.isEmpty()) {
            return Set.of();
        }
        String sql = """
                SELECT DISTINCT e.student_id
                FROM enrollments e
                JOIN classes c ON c.id = e.class_id
                JOIN academic_years ay ON ay.id = e.academic_year_id
                WHERE e.school_id = ? AND ay.active = TRUE AND c.level IN (%s)
                """.formatted(String.join(",", levels.stream().map(l -> "?").toList()));
        List<Object> args = new ArrayList<>();
        args.add(schoolId);
        levels.forEach(l -> args.add(l.name()));
        return new HashSet<>(jdbc.queryForList(sql, String.class, args.toArray()));
    }

    /** null when the caller sees the whole school; otherwise the class sessions (this year) in their sections. */
    public Set<String> classSessionIds(String schoolId) {
        Collection<com.moriba.skultem.domain.vo.Level> levels = sectionScopeService.restrictedLevels();
        if (levels == null) {
            return null;
        }
        if (levels.isEmpty()) {
            return Set.of();
        }
        String sql = """
                SELECT cs.id
                FROM class_sessions cs
                JOIN classes c ON c.id = cs.class_id
                WHERE cs.school_id = ? AND c.level IN (%s)
                """.formatted(String.join(",", levels.stream().map(l -> "?").toList()));
        List<Object> args = new ArrayList<>();
        args.add(schoolId);
        levels.forEach(l -> args.add(l.name()));
        return new HashSet<>(jdbc.queryForList(sql, String.class, args.toArray()));
    }

    /** Can the caller see this student? */
    public boolean allows(String schoolId, String studentId) {
        var ids = studentIds(schoolId);
        return ids == null || (studentId != null && ids.contains(studentId));
    }

    /** Keeps only the records about students the caller may see. */
    public <T> List<T> filter(String schoolId, List<T> records, Function<T, String> studentId) {
        var ids = studentIds(schoolId);
        if (ids == null) {
            return records;
        }
        return records.stream().filter(r -> ids.contains(studentId.apply(r))).toList();
    }
}
