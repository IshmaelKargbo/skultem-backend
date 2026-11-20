package com.moriba.skultem.application.services;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;

import lombok.RequiredArgsConstructor;

/**
 * Which timetable a class runs on - the normal one (periods each day, a lesson in each) or the weekly plan (the
 * term week by week, a subject in each session) - never both. Kept on the class (every section of it, year after
 * year); a class without a choice runs on the normal timetable.
 * <p>
 * Switching keeps the other timetable's data, only hidden: it isn't shown, read or added to while the class is
 * on the other one, and comes back if the school switches back.
 */
@Service
@RequiredArgsConstructor
public class ClassTimetableTypeService {

    public enum Type {
        PERIODS, WEEKLY_PLAN
    }

    // A class and its choice, for the settings list; hasTimetable / hasWeeklyPlan say what each would show.
    // sessionIds: the class's sections (every year), so a screen that only knows a section can tell its type.
    public record ClassType(String classId, String className, Type type, boolean hasTimetable, boolean hasWeeklyPlan,
            List<String> sessionIds) {
    }

    private final JdbcTemplate jdbc;

    public List<ClassType> list(String schoolId) {
        return jdbc.query("""
                SELECT c.id, c.name, ctt.type,
                       (SELECT string_agg(s.id, ',') FROM class_sessions s WHERE s.class_id = c.id) AS session_ids,
                       EXISTS (SELECT 1 FROM timetables t JOIN periods p ON p.id = t.period_id
                               JOIN class_sessions s ON s.id = p.session_id WHERE s.class_id = c.id) AS has_timetable,
                       EXISTS (SELECT 1 FROM weekly_plans wp JOIN class_sessions s ON s.id = wp.class_session_id
                               WHERE s.class_id = c.id) AS has_plan
                FROM classes c
                LEFT JOIN class_timetable_types ctt ON ctt.class_id = c.id
                WHERE c.school_id = ?
                ORDER BY c.name
                """, (rs, i) -> new ClassType(rs.getString("id"), rs.getString("name"), parse(rs.getString("type")),
                rs.getBoolean("has_timetable"), rs.getBoolean("has_plan"), split(rs.getString("session_ids"))), schoolId);
    }

    public Type typeOfClass(String schoolId, String classId) {
        var rows = jdbc.queryForList("SELECT type FROM class_timetable_types WHERE class_id = ? AND school_id = ?",
                String.class, classId, schoolId);
        return rows.isEmpty() ? Type.PERIODS : parse(rows.get(0));
    }

    // The type of the class a class section belongs to; a section that isn't found counts as the normal timetable
    // (the caller's own lookup reports it missing).
    public Type typeOfSession(String schoolId, String classSessionId) {
        var rows = jdbc.queryForList("""
                SELECT ctt.type FROM class_sessions cs
                LEFT JOIN class_timetable_types ctt ON ctt.class_id = cs.class_id
                WHERE cs.id = ? AND cs.school_id = ?
                """, String.class, classSessionId, schoolId);
        return rows.isEmpty() ? Type.PERIODS : parse(rows.get(0));
    }

    public Type typeOfPeriod(String schoolId, String periodId) {
        var rows = jdbc.queryForList("SELECT session_id FROM periods WHERE id = ? AND school_id = ?", String.class,
                periodId, schoolId);
        return rows.isEmpty() ? Type.PERIODS : typeOfSession(schoolId, rows.get(0));
    }

    @Transactional
    public ClassType set(String schoolId, String classId, Type type) {
        var name = jdbc.queryForList("SELECT name FROM classes WHERE id = ? AND school_id = ?", String.class, classId,
                schoolId);
        if (name.isEmpty()) throw new NotFoundException("Class not found");
        jdbc.update("""
                INSERT INTO class_timetable_types (class_id, school_id, type, updated_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (class_id) DO UPDATE SET type = EXCLUDED.type, updated_at = EXCLUDED.updated_at
                """, classId, schoolId, type.name(), Timestamp.from(Instant.now()));
        return list(schoolId).stream().filter(c -> c.classId().equals(classId)).findFirst().orElseThrow();
    }

    // Guards for building one or the other: a class on the weekly plan gets no periods or lessons, and a class on
    // the normal timetable gets no weekly plan.
    public void requirePeriods(String schoolId, String classSessionId) {
        if (typeOfSession(schoolId, classSessionId) != Type.PERIODS) {
            throw new RuleException("This class uses the weekly plan, not the normal timetable. "
                    + "Change it in Timetable settings to build a normal timetable for it.");
        }
    }

    public void requirePeriodsForPeriod(String schoolId, String periodId) {
        if (typeOfPeriod(schoolId, periodId) != Type.PERIODS) {
            throw new RuleException("This class uses the weekly plan, not the normal timetable. "
                    + "Change it in Timetable settings to build a normal timetable for it.");
        }
    }

    public void requireWeeklyPlan(String schoolId, String classSessionId) {
        if (typeOfSession(schoolId, classSessionId) != Type.WEEKLY_PLAN) {
            throw new RuleException("This class uses the normal timetable, not the weekly plan. "
                    + "Change it in Timetable settings to plan it week by week.");
        }
    }

    private static List<String> split(String ids) {
        return ids == null || ids.isBlank() ? List.of() : List.of(ids.split(","));
    }

    private static Type parse(String s) {
        return "WEEKLY_PLAN".equals(s) ? Type.WEEKLY_PLAN : Type.PERIODS;
    }
}
