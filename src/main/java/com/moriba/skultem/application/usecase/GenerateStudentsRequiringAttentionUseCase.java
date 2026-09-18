package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassAcademicAttentionDTO;
import com.moriba.skultem.application.dto.StudentAcademicAttentionDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.service.AttendanceRateCalculator;
import com.moriba.skultem.domain.service.PerformanceTrendCalculator;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Students Requiring Attention - shows each signal separately (low academic average, low
// attendance, missing assessments, declining trend) rather than folding them into one combined
// score, per the requirement to give management evidence rather than an unsupported diagnosis.
// classId == null means whole school. Reuses the exact same attendance-rate and academic-average
// data sources as ComputeClassAttentionUseCase (30-day attendance window, SUBMITTED/RETURNED-
// inclusive academic average) via parallel nullable-classId repository methods, so the two "needs
// attention" views in the app never disagree on the underlying numbers for a single class - this
// one just adds the missing-assessments and trend signals on top, reports every flag individually
// instead of collapsing them, and can also run school-wide.
@Service
@Transactional
@RequiredArgsConstructor
public class GenerateStudentsRequiringAttentionUseCase {

    private static final int ATTENDANCE_WINDOW_DAYS = 30;
    private static final int DEFAULT_PASS_MARK = 50;

    // Deliberately the same exclusion set ComputeClassAttentionUseCase uses (not the stricter
    // APPROVED-only set GetClassAcademicPerformanceUseCase uses) - this is an early-warning view,
    // not the formal released-data report, so a teacher's just-submitted scores should already
    // surface here.
    private static final List<ClassSubjectAssessmentLifeCycle.Status> ACADEMIC_HEURISTIC_EXCLUDED = List.of(
            ClassSubjectAssessmentLifeCycle.Status.DRAFT,
            ClassSubjectAssessmentLifeCycle.Status.LOCKED);

    private final ClassRepository classRepo;
    private final TermRepository termRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final AttendanceRepository attendanceRepo;
    private final AssessmentScoreRepository scoreRepo;
    private final SchoolRepository schoolRepo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public ClassAcademicAttentionDTO execute(String schoolId, String classId, String academicYearId, String termId,
            Level level, int page, int size) {
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));
        double attendanceThreshold = school.getAttendanceThreshold();

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        if (classId != null && !classId.isBlank()) {
            classRepo.findByIdAndSchool(classId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Class not found"));
        }

        Map<String, Integer> passMarkByClass = new HashMap<>();
        Map<String, Level> levelByClass = new HashMap<>();
        for (var clazz : classRepo.findBySchool(schoolId, Pageable.unpaged()).getContent()) {
            passMarkByClass.put(clazz.getId(),
                    clazz.getTemplate() != null ? clazz.getTemplate().getPassMark() : DEFAULT_PASS_MARK);
            levelByClass.put(clazz.getId(), clazz.getLevel());
        }

        Term activeTerm = (termId != null && !termId.isBlank())
                ? termRepo.findByIdAndAcademicYearIdAndSchoolId(termId, academicYear.getId(), schoolId).orElse(null)
                : termRepo.findActiveBySchoolAndAcademicYear(schoolId, academicYear.getId()).orElse(null);

        List<Enrollment> enrollments = classId != null && !classId.isBlank()
                ? enrollmentRepo.findAllByClassAndAcademicAndSchoolId(classId, academicYear.getId(), schoolId,
                        Pageable.unpaged()).getContent()
                : enrollmentRepo.findAllByAcademicSchoolId(academicYear.getId(), schoolId);

        if (level != null) {
            enrollments = enrollments.stream()
                    .filter(e -> level.equals(levelByClass.get(e.getClazz().getId())))
                    .toList();
        }

        Map<String, double[]> attendanceCounts = new HashMap<>(); // enrollmentId -> [present, total]
        LocalDate since = LocalDate.now().minusDays(ATTENDANCE_WINDOW_DAYS);
        for (Object[] row : attendanceRepo.attendanceCountsSinceForReport(schoolId, classId, academicYear.getId(),
                since)) {
            attendanceCounts.put((String) row[0], new double[] {
                    ((Number) row[1]).doubleValue(),
                    ((Number) row[2]).doubleValue()
            });
        }

        Map<String, double[]> academicAverages = new HashMap<>(); // enrollmentId -> [average, count]
        Map<String, long[]> completionByEnrollment = new HashMap<>(); // enrollmentId -> [total, completed]
        Map<String, List<Double>> trendByEnrollment = new HashMap<>();

        if (activeTerm != null) {
            for (Object[] row : scoreRepo.averageScoresForAttentionReport(schoolId, classId, activeTerm.getId(),
                    ACADEMIC_HEURISTIC_EXCLUDED)) {
                academicAverages.put((String) row[0], new double[] {
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).doubleValue()
                });
            }

            for (Object[] row : scoreRepo.assessmentCompletionByClassAndTerm(schoolId, classId, activeTerm.getId(),
                    GetClassAcademicPerformanceUseCase.APPROVED_STATUSES)) {
                completionByEnrollment.put((String) row[0], new long[] {
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue()
                });
            }

            for (Object[] row : scoreRepo.assessmentTrendByClassAndTerm(schoolId, classId, activeTerm.getId(),
                    GetClassAcademicPerformanceUseCase.APPROVED_STATUSES)) {
                String enrollmentId = (String) row[0];
                double avg = ((Number) row[2]).doubleValue();
                trendByEnrollment.computeIfAbsent(enrollmentId, k -> new ArrayList<>()).add(avg);
            }
        }

        var flagged = new ArrayList<StudentAcademicAttentionDTO>();

        for (var enrollment : enrollments) {
            int passMark = passMarkByClass.getOrDefault(enrollment.getClazz().getId(), DEFAULT_PASS_MARK);

            var attendance = attendanceCounts.get(enrollment.getId());
            Double attendanceRate = (attendance != null && attendance[1] > 0)
                    ? AttendanceRateCalculator.rate((long) attendance[0], (long) attendance[1])
                    : null;
            boolean lowAttendanceSignal = AttendanceRateCalculator.isBelowThreshold(attendanceRate,
                    attendanceThreshold);

            var academic = academicAverages.get(enrollment.getId());
            Double academicAverage = (academic != null && academic[1] > 0)
                    ? Math.round(academic[0] * 10.0) / 10.0
                    : null;
            boolean lowAcademicSignal = academicAverage != null && academicAverage < passMark;

            long[] completion = completionByEnrollment.getOrDefault(enrollment.getId(), new long[] { 0, 0 });
            long missingAssessments = Math.max(0, completion[0] - completion[1]);
            boolean missingAssessmentsSignal = missingAssessments > 0;

            var trend = PerformanceTrendCalculator.evaluate(trendByEnrollment.get(enrollment.getId()));
            boolean decliningTrendSignal = trend == PerformanceTrendCalculator.Trend.DECLINING;

            if (!lowAttendanceSignal && !lowAcademicSignal && !missingAssessmentsSignal && !decliningTrendSignal) {
                continue;
            }

            var student = enrollment.getStudent();
            flagged.add(new StudentAcademicAttentionDTO(
                    student.getId(),
                    enrollment.getId(),
                    student.getGivenNames(),
                    student.getFamilyName(),
                    student.getPhoto(),
                    academicAverage,
                    attendanceRate,
                    missingAssessments,
                    trend,
                    lowAcademicSignal,
                    lowAttendanceSignal,
                    missingAssessmentsSignal,
                    decliningTrendSignal));
        }

        // Sort deterministically before paginating, same rationale as
        // GetClassAcademicPerformanceUseCase's student list.
        flagged.sort(Comparator
                .comparing(StudentAcademicAttentionDTO::familyName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(StudentAcademicAttentionDTO::givenNames, String.CASE_INSENSITIVE_ORDER));

        int flaggedCount = flagged.size();
        int safeSize = size > 0 ? size : flaggedCount;
        int safePage = Math.max(page, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, flaggedCount);
        int toIndex = Math.min(fromIndex + safeSize, flaggedCount);

        return new ClassAcademicAttentionDTO(enrollments.size(), flaggedCount, safePage, safeSize,
                flagged.subList(fromIndex, toIndex));
    }
}
