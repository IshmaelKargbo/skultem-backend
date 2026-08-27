package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.GenerateReportCardsDTO;
import com.moriba.skultem.application.dto.GenerateReportCardsResultDTO;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.ReportCardAssessmentScoreDTO;
import com.moriba.skultem.application.dto.ReportCardSubjectDTO;
import com.moriba.skultem.application.dto.ReportCardSummaryDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ReportCardMapper;
import com.moriba.skultem.domain.model.ReportCard;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ReportCardRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.FilterOperator;
import com.moriba.skultem.domain.vo.GradeBand;
import com.moriba.skultem.infrastructure.persistence.mapper.JsonMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Orchestrates existing, already-correct reporting primitives (grade rows,
// class rank, attendance history) into one persisted snapshot per student -
// this deliberately does not recompute grades/attendance/rank itself, so it
// can't drift from what the grades and attendance features already show.
@Service
@Transactional
@RequiredArgsConstructor
public class GenerateReportCardsUseCase {

    private final ListEnrollmentByClassUseCase listEnrollmentByClassUseCase;
    private final GradeReportUseCase gradeReportUseCase;
    private final RankStudentUseCase rankStudentUseCase;
    private final AttendanceRepository attendanceRepo;
    private final ClassRepository classRepo;
    private final TermRepository termRepo;
    private final SchoolRepository schoolRepo;
    private final ReportCardRepository reportCardRepo;

    public GenerateReportCardsResultDTO execute(String schoolId, String userId, GenerateReportCardsDTO param) {
        var term = termRepo.findByIdAndSchoolId(param.termId(), schoolId)
                .orElseThrow(() -> new NotFoundException("Term not found"));

        var clazz = classRepo.findByIdAndSchool(param.classId(), schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        var school = schoolRepo.findById(schoolId).orElse(null);
        List<GradeBand> gradingScale = school != null && school.getGradingScale() != null
                ? school.getGradingScale()
                : List.of();
        int passMark = clazz.getTemplate() != null ? clazz.getTemplate().getPassMark() : 50;

        var roster = listEnrollmentByClassUseCase.execute(schoolId, param.classId(), term.getAcademicYear().getId(),
                0, 0);

        // ListEnrollmentByClassUseCase maps students without a class-size lookup
        // (its 2-arg StudentMapper.toDTO overload leaves classSize at 0) - since
        // we already have the full roster in hand, its own size is the correct
        // value, not whatever the DTO happened to carry.
        int classSize = roster.getContent().size();

        List<ReportCardSummaryDTO> generated = new ArrayList<>();
        double totalAverage = 0;
        long passedCount = 0;

        for (StudentDTO student : roster.getContent()) {
            var scoreRows = gradeReportUseCase.execute(new ReportBuilderDTO(schoolId, "grades", List.of(
                    new Filter("studentAssessment.enrollment.student.id", FilterOperator.EQUALS, "select",
                            student.id(), null, null),
                    new Filter("cycle.term.id", FilterOperator.EQUALS, "select", param.termId(), null, null))),
                    1, 500).getContent();

            // Nothing submitted for this student this term yet - skip rather than
            // persist a blank snapshot; "Generate" only covers students who actually
            // have grades to report.
            if (scoreRows.isEmpty()) continue;

            // scoreRows is one row per assessment (Test 1, Test 2, Exam, ...) per
            // subject - a report card shows one row per SUBJECT with its cumulative
            // weighted total, but keeps the per-assessment breakdown too (this is a
            // term report, so which test contributed what still matters).
            Map<String, Double> weightedTotalBySubject = new LinkedHashMap<>();
            Map<String, String> teacherBySubject = new LinkedHashMap<>();
            Map<String, List<ReportCardAssessmentScoreDTO>> assessmentsBySubject = new LinkedHashMap<>();

            for (var row : scoreRows) {
                weightedTotalBySubject.merge(row.subject(), (double) (row.weightScore() != null ? row.weightScore() : 0),
                        Double::sum);
                teacherBySubject.putIfAbsent(row.subject(), row.teacher());
                assessmentsBySubject.computeIfAbsent(row.subject(), k -> new ArrayList<>())
                        .add(new ReportCardAssessmentScoreDTO(row.name(), row.score(), row.weight(), row.level()));
            }

            var subjects = weightedTotalBySubject.entrySet().stream()
                    .map(e -> {
                        int subjectScore = (int) Math.round(e.getValue());
                        var assessments = assessmentsBySubject.get(e.getKey()).stream()
                                .sorted(Comparator.comparingInt(a -> a.level() != null ? a.level() : Integer.MAX_VALUE))
                                .toList();
                        return new ReportCardSubjectDTO(e.getKey(), teacherBySubject.get(e.getKey()), subjectScore,
                                100, subjectScore, resolveGrade(gradingScale, subjectScore), assessments);
                    })
                    .toList();

            double average = subjects.stream()
                    .mapToInt(ReportCardSubjectDTO::score)
                    .average()
                    .orElse(0);

            int position = param.includeRanking() ? rankStudentUseCase.execute(student.id(), param.termId(), schoolId)
                    : 0;

            String overallGrade = resolveGrade(gradingScale, average);
            boolean passed = average >= passMark;

            Double attendancePercentage = param.includeAttendance()
                    ? computeAttendancePercentage(student.enrollmentId(), schoolId, term.getStartDate(),
                            term.getEndDate())
                    : null;

            var subjectsJson = JsonMapper.toJson(subjects);
            var studentName = (student.givenNames() + " " + student.familyName()).trim();
            var academicYearName = term.getAcademicYear() != null ? term.getAcademicYear().getName() : "";

            var existing = reportCardRepo.findBySchoolIdAndStudentIdAndTermId(schoolId, student.id(), param.termId())
                    .orElse(null);

            ReportCard card;
            if (existing != null) {
                existing.regenerate(studentName, student.admissionNumber(), student.photo(), student.className(),
                        classSize, term.getName(), academicYearName, average, position, overallGrade, passed,
                        attendancePercentage, subjectsJson, userId);
                card = existing;
            } else {
                card = ReportCard.generate(UUID.randomUUID().toString(), schoolId, student.id(), studentName,
                        student.admissionNumber(), student.photo(), param.classId(), student.className(), classSize,
                        param.termId(), term.getName(), academicYearName, average, position, overallGrade, passed,
                        attendancePercentage, null, subjectsJson, userId);
            }

            reportCardRepo.save(card);
            generated.add(ReportCardMapper.toSummaryDTO(card));

            totalAverage += average;
            if (passed) passedCount++;
        }

        double classAverage = generated.isEmpty() ? 0 : totalAverage / generated.size();

        return new GenerateReportCardsResultDTO(generated.size(), passedCount, generated.size() - passedCount,
                classAverage, generated);
    }

    private String resolveGrade(List<GradeBand> bands, double average) {
        return bands.stream()
                .filter(b -> average >= b.minScore() && average <= b.maxScore())
                .map(GradeBand::grade)
                .findFirst()
                .orElse("N/A");
    }

    private Double computeAttendancePercentage(String enrollmentId, String schoolId, LocalDate start,
            LocalDate end) {
        if (enrollmentId == null) return null;

        var records = attendanceRepo.findByEnrollmentAndSchoolId(enrollmentId, schoolId, Pageable.unpaged())
                .getContent().stream()
                .filter(a -> !a.isHoliday())
                .filter(a -> !a.getDate().isBefore(start) && !a.getDate().isAfter(end))
                .toList();

        if (records.isEmpty()) return null;

        long present = records.stream().filter(a -> a.isPresent() || a.isLate()).count();
        return Math.round((present * 1000.0) / records.size()) / 10.0;
    }
}
