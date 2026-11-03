package com.moriba.skultem.application.usecase;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ExamSubmissionDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// The exam office's submission tracker: every open assessment this term (one per class session x subject x
// assessment), with the teacher who owns it and its status. Assessments not started yet (LOCKED) are left out -
// nothing is owed on them.
@Service
@Transactional
@RequiredArgsConstructor
public class ListExamSubmissionsUseCase {

    private final JdbcTemplate jdbc;
    private final TermRepository termRepo;
    private final AcademicYearRepository academicYearRepo;
    private final SectionScopeService sectionScopeService;

    public record Result(String termId, String termName, List<ExamSubmissionDTO> rows) {
    }

    public Result execute(String schoolId, String termId) {
        var term = termId != null && !termId.isBlank()
                ? termRepo.findByIdAndSchoolId(termId, schoolId)
                        .orElseThrow(() -> new NotFoundException("Term not found"))
                : academicYearRepo.findActiveBySchool(schoolId)
                        .flatMap(y -> termRepo.findActiveBySchoolAndAcademicYear(schoolId, y.getId()))
                        .orElseThrow(() -> new NotFoundException("There is no active term"));

        var levels = sectionScopeService.levels();
        if (levels.isEmpty()) {
            return new Result(term.getId(), term.getName(), List.of());
        }

        var sql = """
                SELECT cy.id, cs.id AS session_id, c.id AS class_id, c.name AS class_name, sec.name AS section_name,
                       st.name AS stream_name, c.level, s.name AS subject_name, a.name AS assessment_name,
                       a.position, t.id AS teacher_id,
                       trim(coalesce(u.given_name, '') || ' ' || coalesce(u.family_name, '')) AS teacher_name,
                       cy.status, cy.updated_at
                FROM class_subject_assessment_life_cycle cy
                JOIN teacher_subjects ts ON ts.id = cy.teacher_subject_id
                JOIN class_sessions cs ON cs.id = ts.class_session_id
                JOIN classes c ON c.id = cs.class_id
                LEFT JOIN sections sec ON sec.id = cs.section_id
                LEFT JOIN streams st ON st.id = cs.stream_id
                JOIN subjects s ON s.id = ts.subject_id
                JOIN assessments a ON a.id = cy.assessment_id
                LEFT JOIN teachers t ON t.id = ts.teacher_id
                LEFT JOIN users u ON u.id = t.user_id
                WHERE cy.school_id = ? AND cy.term_id = ? AND cy.status <> 'LOCKED'
                  AND c.level IN (%s)
                ORDER BY c.level_order, c.name, sec.name, s.name, a.position
                """.formatted(String.join(",", levels.stream().map(l -> "?").toList()));
        List<Object> args = new ArrayList<>(List.of(schoolId, term.getId()));
        levels.forEach(l -> args.add(l.name()));

        var rows = jdbc.query(sql, (rs, i) -> {
            // Same shape as ClassSessionLabel: "JSS 1 · A", "SSS 2 · B · Science".
            var name = new StringBuilder(rs.getString("class_name"));
            var section = rs.getString("section_name");
            if (section != null) {
                name.append(" · ").append(section);
            }
            var stream = rs.getString("stream_name");
            if (stream != null && !"N/A".equalsIgnoreCase(stream)) {
                name.append(" · ").append(stream);
            }
            var teacherName = rs.getString("teacher_name");
            Timestamp updated = rs.getTimestamp("updated_at");
            return new ExamSubmissionDTO(rs.getString("id"), rs.getString("session_id"), rs.getString("class_id"),
                    name.toString(), rs.getString("level"), rs.getString("subject_name"),
                    rs.getString("assessment_name"), rs.getInt("position"), rs.getString("teacher_id"),
                    teacherName == null || teacherName.isBlank() ? null : teacherName, rs.getString("status"),
                    updated == null ? null : updated.toInstant());
        }, args.toArray());

        return new Result(term.getId(), term.getName(), rows);
    }
}
