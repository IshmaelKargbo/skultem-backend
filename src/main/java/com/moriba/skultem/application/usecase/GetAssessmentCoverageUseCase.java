package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.AssessmentCoverageDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.repository.AcademicYearRepository;

import lombok.RequiredArgsConstructor;

// The Academics "Assessment check": which classes have students without the assessments their template should
// have given them, and why (no template, a template with no tests, or a subject with no teacher). The fix is
// SyncClassAssessmentsUseCase, run per class from the same screen.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAssessmentCoverageUseCase {

    private final JdbcTemplate jdbc;
    private final AcademicYearRepository academicYearRepo;
    private final com.moriba.skultem.domain.repository.TermRepository termRepo;
    private final SectionScopeService sectionScopeService;

    public AssessmentCoverageDTO execute(String schoolId) {
        var year = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));
        Integer activeTerm = termRepo.findByAcademicYearIdAndSchool(year.getId(), schoolId).stream()
                .filter(t -> t.getStatus() == com.moriba.skultem.domain.model.Term.Status.ACTIVE)
                .map(t -> t.getTermNumber()).findFirst().orElse(null);
        var levels = sectionScopeService.levels();
        if (levels.isEmpty()) {
            return new AssessmentCoverageDTO(year.getName(), activeTerm, List.of());
        }

        String sql = """
                WITH per_student AS (
                    SELECT e.class_id, e.id AS enrollment_id,
                           (SELECT COUNT(*) FROM student_assessments sa WHERE sa.enrollment_id = e.id) AS n
                    FROM enrollments e
                    WHERE e.school_id = ? AND e.academic_year_id = ? AND e.status = 'ACTIVE'
                ),
                per_class AS (
                    SELECT class_id, COUNT(*) AS students,
                           COUNT(*) FILTER (WHERE n = 0) AS without_any,
                           MAX(n) AS most
                    FROM per_student GROUP BY class_id
                )
                SELECT c.id, c.name, c.level, t.id AS template_id, t.name AS template_name,
                       (SELECT COUNT(*) FROM assessments a WHERE a.template_id = t.id) AS template_tests,
                       COALESCE(t.starts_in_term, 1) AS starts_in_term,
                       (SELECT COUNT(*) FROM class_subjects cs WHERE cs.class_id = c.id) AS subjects,
                       COALESCE(pc.students, 0) AS students,
                       COALESCE(pc.without_any, 0) AS without_any,
                       (SELECT COUNT(*) FROM per_student ps
                        WHERE ps.class_id = c.id AND ps.n > 0 AND ps.n < pc.most) AS incomplete,
                       (SELECT string_agg(DISTINCT s.name, '|')
                        FROM class_subjects cs JOIN subjects s ON s.id = cs.subject_id
                        WHERE cs.class_id = c.id AND cs.mandatory = TRUE
                          AND NOT EXISTS (SELECT 1 FROM teacher_subjects ts
                                          JOIN class_sessions ss ON ss.id = ts.class_session_id
                                          WHERE ss.class_id = c.id AND ss.academic_year_id = ?
                                            AND ts.subject_id = cs.subject_id)) AS no_teacher
                FROM classes c
                LEFT JOIN assessment_templates t ON t.id = c.assessment_template_id
                LEFT JOIN per_class pc ON pc.class_id = c.id
                WHERE c.school_id = ? AND c.status = 'ACTIVE' AND c.level IN (%s)
                ORDER BY c.level_order, c.name
                """.formatted(String.join(",", levels.stream().map(l -> "?").toList()));

        List<Object> args = new ArrayList<>(List.of(schoolId, year.getId(), year.getId(), schoolId));
        levels.forEach(l -> args.add(l.name()));

        var rows = jdbc.query(sql, (rs, i) -> {
            int students = rs.getInt("students");
            int withoutAny = rs.getInt("without_any");
            int incomplete = rs.getInt("incomplete");
            int tests = rs.getInt("template_tests");
            int subjects = rs.getInt("subjects");
            int startsInTerm = rs.getInt("starts_in_term");
            String templateId = rs.getString("template_id");
            String noTeacher = rs.getString("no_teacher");
            List<String> subjectsWithoutTeacher = noTeacher == null ? List.of() : Arrays.asList(noTeacher.split("\\|"));

            String status;
            if (students == 0) status = "NO_STUDENTS";
            else if (templateId == null) status = "NO_TEMPLATE";
            else if (tests == 0) status = "EMPTY_TEMPLATE";
            else if (subjects == 0 && withoutAny > 0) status = "NO_SUBJECTS";
            else if (withoutAny > 0) status = "MISSING";
            else if (incomplete > 0 || !subjectsWithoutTeacher.isEmpty()) status = "INCOMPLETE";
            else if (activeTerm != null && startsInTerm > activeTerm) status = "LATER_START";
            else status = "OK";

            return new AssessmentCoverageDTO.Row(rs.getString("id"), rs.getString("name"), rs.getString("level"),
                    templateId, rs.getString("template_name"), tests, startsInTerm, subjects, students, withoutAny,
                    incomplete, subjectsWithoutTeacher, status);
        }, args.toArray());

        return new AssessmentCoverageDTO(year.getName(), activeTerm, rows);
    }
}
