package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.GradebookDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import lombok.RequiredArgsConstructor;

// The class master's grade book: one class section, one term, every pupil against every subject - live, as subject
// teachers submit, rather than waiting for report cards (the broadsheet reads those). Only submitted marks (awaiting
// approval, approved or completed) are shown; a draft or returned assessment is listed as not submitted, its marks
// hidden. Assessments not yet started (LOCKED) are left out - nothing is owed on them. Read in two queries.
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetClassGradebookUseCase {

    private static final int DEFAULT_PASS_MARK = 50;
    private static final Set<String> RELEASED = Set.of("SUBMITTED", "APPROVED", "COMPLETED");

    // One submitted-or-not mark: a pupil, a subject, an assessment.
    record Line(String tsId, String subject, String subjectCode, String teacher, String assessmentId, String assessmentName,
            int position, Integer weight, String status, String studentId, String studentName,
            String admissionNumber, String photo, Integer score) {
    }

    static final String CLASS_SQL = """
                SELECT c.id AS class_id, cs.section_id, cs.stream_id, c.name AS class_name, sec.name AS section_name,
                       st.name AS stream_name, at.pass_mark
                FROM class_sessions cs
                JOIN classes c ON c.id = cs.class_id
                LEFT JOIN sections sec ON sec.id = cs.section_id
                LEFT JOIN streams st ON st.id = cs.stream_id
                LEFT JOIN assessment_templates at ON at.id = c.assessment_template_id
                WHERE cs.id = ? AND cs.school_id = ?
                """;

    static final String LINES_SQL = """
                SELECT ts.id AS ts_id, sub.name AS subject_name, sub.code AS subject_code,
                       trim(coalesce(u.given_name, '') || ' ' || coalesce(u.family_name, '')) AS teacher_name,
                       a.id AS assessment_id, a.name AS assessment_name, a.position, sc.weight, cy.status,
                       stu.id AS student_id,
                       trim(coalesce(stu.given_names, '') || ' ' || coalesce(stu.family_name, '')) AS student_name,
                       stu.admission_number, stu.photo, sc.score
                FROM assessment_scores sc
                JOIN class_subject_assessment_life_cycle cy ON cy.id = sc.class_subject_assessment_life_cycle_id
                JOIN assessments a ON a.id = cy.assessment_id
                JOIN teacher_subjects ts ON ts.id = cy.teacher_subject_id
                JOIN subjects sub ON sub.id = ts.subject_id
                JOIN student_assessments sa ON sa.id = sc.student_assessment_id
                JOIN enrollments e ON e.id = sa.enrollment_id
                JOIN students stu ON stu.id = e.student_id
                LEFT JOIN teachers t ON t.id = ts.teacher_id
                LEFT JOIN users u ON u.id = t.user_id
                WHERE sc.school_id = ? AND ts.class_session_id = ? AND cy.term_id = ?
                  AND cy.status <> 'LOCKED' AND e.status <> 'LEFT'
                ORDER BY sub.name, a.position
                """;

    private final JdbcTemplate jdbc;
    private final TermRepository termRepo;
    private final AcademicYearRepository academicYearRepo;
    private final ResolveScoreGradeUseCase resolveScoreGradeUseCase;

    public GradebookDTO execute(String schoolId, String classSessionId, String termId) {
        var term = termId != null && !termId.isBlank()
                ? termRepo.findByIdAndSchoolId(termId, schoolId)
                        .orElseThrow(() -> new NotFoundException("Term not found"))
                : academicYearRepo.findActiveBySchool(schoolId)
                        .flatMap(y -> termRepo.findActiveBySchoolAndAcademicYear(schoolId, y.getId()))
                        .orElseThrow(() -> new NotFoundException("There is no active term"));

        var classInfo = jdbc.query(CLASS_SQL, (rs, i) -> {
            var name = new StringBuilder(rs.getString("class_name"));
            var section = rs.getString("section_name");
            if (section != null) {
                name.append(" · ").append(section);
            }
            var stream = rs.getString("stream_name");
            if (stream != null && !"N/A".equalsIgnoreCase(stream)) {
                name.append(" · ").append(stream);
            }
            int pass = rs.getInt("pass_mark");
            return new Object[] { name.toString(), rs.wasNull() ? DEFAULT_PASS_MARK : pass, rs.getString("class_id"),
                    rs.getString("section_id"), rs.getString("stream_id") };
        }, classSessionId, schoolId).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Class not found"));
        String className = (String) classInfo[0];
        int passMark = (Integer) classInfo[1];

        var lines = jdbc.query(LINES_SQL, (rs, i) -> new Line(rs.getString("ts_id"), rs.getString("subject_name"),
                blankToNull(rs.getString("subject_code")),
                blankToNull(rs.getString("teacher_name")), rs.getString("assessment_id"),
                rs.getString("assessment_name"), rs.getInt("position"), (Integer) rs.getObject("weight"),
                rs.getString("status"), rs.getString("student_id"), rs.getString("student_name"),
                rs.getString("admission_number"), rs.getString("photo"), (Integer) rs.getObject("score")),
                schoolId, classSessionId, term.getId());

        var book = assemble(classSessionId, className, term.getId(), term.getName(), passMark, lines,
                score -> resolveScoreGradeUseCase.execute(schoolId, score));
        return new GradebookDTO(book.classSessionId(), book.className(), book.termId(), book.termName(),
                book.passMark(), book.subjects(), book.students(), (String) classInfo[2], (String) classInfo[3],
                (String) classInfo[4]);
    }

    // Everything after the two reads, kept apart so it can be tested on its own: subjects and their assessments,
    // each pupil's submitted marks and totals, averages and positions.
    static GradebookDTO assemble(String classSessionId, String className, String termId, String termName,
            int passMark, List<Line> lines, java.util.function.IntFunction<String> gradeOf) {
        // Subjects (in name order) and their assessments (in position order), each with its status.
        var subjects = new LinkedHashMap<String, Line>();
        var assessmentsBySubject = new LinkedHashMap<String, LinkedHashMap<String, Line>>();
        // Pupils, and each one's lines by subject.
        var pupils = new LinkedHashMap<String, Line>();
        var linesByPupil = new HashMap<String, Map<String, List<Line>>>();
        for (var l : lines) {
            subjects.putIfAbsent(l.tsId(), l);
            assessmentsBySubject.computeIfAbsent(l.tsId(), k -> new LinkedHashMap<>()).putIfAbsent(l.assessmentId(), l);
            pupils.putIfAbsent(l.studentId(), l);
            linesByPupil.computeIfAbsent(l.studentId(), k -> new HashMap<>())
                    .computeIfAbsent(l.tsId(), k -> new ArrayList<>()).add(l);
        }

        var students = new ArrayList<GradebookDTO.Student>();
        var totalsBySubject = new HashMap<String, List<Double>>();
        for (var p : pupils.values()) {
            var results = new LinkedHashMap<String, GradebookDTO.SubjectResult>();
            var subjectTotals = new ArrayList<Double>();
            for (var tsId : subjects.keySet()) {
                var own = linesByPupil.get(p.studentId()).getOrDefault(tsId, List.of());
                var scores = new LinkedHashMap<String, Integer>();
                double weighted = 0, weights = 0;
                for (var l : own) {
                    if (!RELEASED.contains(l.status())) {
                        continue; // Not handed in: the teacher's draft stays theirs.
                    }
                    scores.put(l.assessmentId(), l.score());
                    if (l.score() != null) {
                        int w = l.weight() == null || l.weight() <= 0 ? 1 : l.weight();
                        weighted += l.score() * (double) w;
                        weights += w;
                    }
                }
                Double total = weights > 0 ? round1(weighted / weights) : null;
                String grade = total == null ? null : gradeOf.apply((int) Math.round(total));
                results.put(tsId, new GradebookDTO.SubjectResult(total, grade, scores));
                if (total != null) {
                    subjectTotals.add(total);
                    totalsBySubject.computeIfAbsent(tsId, k -> new ArrayList<>()).add(total);
                }
            }
            Double average = subjectTotals.isEmpty() ? null
                    : round1(subjectTotals.stream().mapToDouble(Double::doubleValue).average().orElse(0));
            students.add(new GradebookDTO.Student(p.studentId(), p.studentName(), p.admissionNumber(), p.photo(),
                    results, average, null, average == null ? null : average >= passMark));
        }

        // Position by average; equal averages share a place (1, 2, 2, 4). No marks yet, no position.
        students.sort(Comparator.comparing((GradebookDTO.Student s) -> s.average() == null ? -1 : s.average())
                .reversed().thenComparing(GradebookDTO.Student::name, String.CASE_INSENSITIVE_ORDER));
        var ranked = new ArrayList<GradebookDTO.Student>();
        Integer position = null;
        Double previous = null;
        for (int i = 0; i < students.size(); i++) {
            var s = students.get(i);
            if (s.average() != null && !s.average().equals(previous)) {
                position = i + 1;
                previous = s.average();
            }
            ranked.add(new GradebookDTO.Student(s.studentId(), s.name(), s.admissionNumber(), s.photo(),
                    s.subjects(), s.average(), s.average() == null ? null : position, s.passed()));
        }

        var subjectDTOs = new ArrayList<GradebookDTO.Subject>();
        for (var e : subjects.entrySet()) {
            var assessments = assessmentsBySubject.get(e.getKey()).values().stream()
                    .sorted(Comparator.comparingInt(Line::position))
                    .map(l -> new GradebookDTO.Assessment(l.assessmentId(), l.assessmentName(), l.position(),
                            l.weight(), statusOf(l.status())))
                    .toList();
            int submitted = (int) assessments.stream().filter(a -> !"NOT_SUBMITTED".equals(a.status())).count();
            var totals = totalsBySubject.getOrDefault(e.getKey(), List.of());
            Double classAverage = totals.isEmpty() ? null
                    : round1(totals.stream().mapToDouble(Double::doubleValue).average().orElse(0));
            subjectDTOs.add(new GradebookDTO.Subject(e.getKey(), e.getValue().subject(),
                    e.getValue().subjectCode(), e.getValue().teacher(),
                    assessments, submitted, classAverage));
        }

        return new GradebookDTO(classSessionId, className, termId, termName, passMark, subjectDTOs, ranked, null, null,
                null);
    }

    private static String statusOf(String cycleStatus) {
        return switch (cycleStatus) {
            case "APPROVED", "COMPLETED" -> "APPROVED";
            case "SUBMITTED" -> "SUBMITTED";
            default -> "NOT_SUBMITTED";
        };
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
