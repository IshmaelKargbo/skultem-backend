package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A student's position in their class for a term - the one ranking rule behind every "Class Rank" (the rank
// endpoint: student performance page, parent dashboard and views; the class page orders its table the same way):
//   1. higher average of marked, approved scores that term (AssessmentScore#hasSettledScore - drafts, submitted
//      and never-marked locked tests don't count)
//   2. equal averages: higher term attendance, over every school day so far (GetStudentTermAttendanceUseCase,
//      the figure the class page shows)
//   3. equal on both: name order
// Positions run 1, 2, 3... and are never shared. A student with no marked score yet has no position (0).
@Service
@Transactional
@RequiredArgsConstructor
public class RankStudentUseCase {

    private final StudentRepository studentRepo;
    private final TermRepository termRepo;
    private final AssessmentScoreRepository assessmentScoreRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final GetStudentTermAttendanceUseCase getStudentTermAttendanceUseCase;

    public record Standing(String studentId, String name, double average, int scoreCount, Double attendance,
            int position) {
    }

    public int execute(String studentId, String termId, String schoolId) {

        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        var term = termRepo.findByIdAndSchoolId(termId, schoolId)
                .orElseThrow(() -> new NotFoundException("Term not found"));

        // The class they were in that year - not necessarily their current one for an earlier year's term.
        var clazzId = enrollmentRepo
                .findByStudentAndAcademicYearAndSchoolId(studentId, term.getAcademicYear().getId(), schoolId)
                .map(e -> e.getClazz().getId())
                .orElseGet(() -> student.getSession().getClazz().getId());

        return standings(schoolId, clazzId, term).stream()
                .filter(s -> s.studentId().equals(studentId))
                .mapToInt(Standing::position)
                .findFirst()
                .orElse(0);
    }

    // Everyone in the class with a marked score that term, best first.
    public List<Standing> standings(String schoolId, String classId, Term term) {
        var attendance = attendanceByStudent(schoolId, classId, term);

        record Row(String studentId, String name, double average, int count, Double attendance) {
        }
        var rows = new ArrayList<Row>();
        // [enrollmentId, studentId, givenNames, familyName, average, count] - marked, settled scores only.
        for (Object[] r : assessmentScoreRepo.settledAverageScoresByClassAndTerm(schoolId, classId, term.getId())) {
            String id = (String) r[1];
            String name = ((r[2] == null ? "" : r[2]) + " " + (r[3] == null ? "" : r[3])).trim();
            double avg = ((Number) r[4]).doubleValue();
            // Rounded as the class page shows it, so "equal" means equal on screen too.
            rows.add(new Row(id, name, Math.round(avg * 10.0) / 10.0, ((Number) r[5]).intValue(), attendance.get(id)));
        }

        rows.sort(Comparator.comparingDouble(Row::average).reversed()
                .thenComparing(r -> r.attendance() == null ? -1.0 : r.attendance(), Comparator.reverseOrder())
                .thenComparing(Row::name, String.CASE_INSENSITIVE_ORDER));

        var out = new ArrayList<Standing>();
        for (int i = 0; i < rows.size(); i++) {
            var r = rows.get(i);
            out.add(new Standing(r.studentId(), r.name(), r.average(), r.count(), r.attendance(), i + 1));
        }
        return out;
    }

    private Map<String, Double> attendanceByStudent(String schoolId, String classId, Term term) {
        // The term and its own year are passed, so this resolves without throwing; a term with no school days yet
        // just leaves every percentage null (ranked on averages and names alone).
        var out = new HashMap<String, Double>();
        getStudentTermAttendanceUseCase.execute(schoolId, term.getAcademicYear().getId(), term.getId(), classId)
                .students().forEach(r -> {
                    if (r.percentage() != null)
                        out.put(r.studentId(), r.percentage());
                });
        return out;
    }
}
