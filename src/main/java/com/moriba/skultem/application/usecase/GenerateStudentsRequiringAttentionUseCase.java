package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.services.SectionScopeService;

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
import com.moriba.skultem.application.services.AttendanceRulesResolver;
import com.moriba.skultem.domain.service.AcademicAttentionCalculator;
import com.moriba.skultem.domain.service.AttendanceAttentionCalculator;
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

    // See ComputeClassAttentionUseCase - attendance is judged by the same calculator over the same rows.
    private static final int MIN_LOOKBACK_DAYS = 90;
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
    private final SectionScopeService sectionScopeService;
    private final AttendanceRulesResolver attendanceRulesResolver;

    // Calendar days of attendance to fetch: two windows of school days (~5 per 7 calendar days), with
    // slack for breaks - never less than MIN_LOOKBACK_DAYS so term to date is covered too.
    private static int lookbackDays(AttendanceAttentionCalculator.Rules rules) {
        return Math.max(MIN_LOOKBACK_DAYS, rules.windowDays() * 4);
    }

    public ClassAcademicAttentionDTO execute(String schoolId, String classId, String academicYearId, String termId,
            Level level, int page, int size) {
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));
        // Each section may have its own attendance rules; levels without an override use the school's.
        var schoolRules = school.attendanceRules();
        var rulesByLevel = attendanceRulesResolver.forAllLevels(school);

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        if (classId != null && !classId.isBlank()) {
            classRepo.findByIdAndSchool(classId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Class not found"));
        }

        // Only the caller's own management-section classes go into this map (whole catalog for a
        // whole-school caller) - everything downstream keys off it, so this one restriction is what
        // keeps a section-limited admin's "whole school" view actually limited to their sections.
        Map<String, Integer> passMarkByClass = new HashMap<>();
        Map<String, Level> levelByClass = new HashMap<>();
        for (var clazz : classRepo.findBySchool(schoolId, sectionScopeService.levels(), Pageable.unpaged())
                .getContent()) {
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

        // In scope regardless of the level filter above - out-of-scope classes were never added to
        // levelByClass, so containsKey doubles as the section-scope check.
        enrollments = enrollments.stream().filter(e -> levelByClass.containsKey(e.getClazz().getId())).toList();
        if (level != null) {
            enrollments = enrollments.stream()
                    .filter(e -> level.equals(levelByClass.get(e.getClazz().getId())))
                    .toList();
        }

        int lookback = rulesByLevel.values().stream().mapToInt(GenerateStudentsRequiringAttentionUseCase::lookbackDays).max().orElse(0);
        lookback = Math.max(lookback, lookbackDays(schoolRules));
        LocalDate since = LocalDate.now().minusDays(lookback);
        LocalDate termStart = activeTerm != null ? activeTerm.getStartDate() : null;
        if (termStart != null && termStart.isBefore(since)) {
            since = termStart;
        }
        var daysByEnrollment = AttendanceAttentionCalculator.groupByEnrollment(
                attendanceRepo.attendanceDaysSince(schoolId, classId, academicYear.getId(), since));

        Map<String, double[]> academicAverages = new HashMap<>(); // enrollmentId -> [average, count]
        Map<String, double[]> previousAverages = new HashMap<>(); // same, for the term before
        Map<String, long[]> completionByEnrollment = new HashMap<>(); // enrollmentId -> [total, completed]
        Map<String, List<Double>> trendByEnrollment = new HashMap<>();

        if (activeTerm != null) {
            for (Object[] row : scoreRepo.averageScoresForAttentionReport(schoolId, classId, activeTerm.getId(),
                    sectionScopeService.levels(), ACADEMIC_HEURISTIC_EXCLUDED)) {
                academicAverages.put((String) row[0], new double[] {
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).doubleValue()
                });
            }

            // The term before this one in the same academic year, only to tell recovering from sliding.
            var previousTerm = activeTerm.getTermNumber() > 1
                    ? termRepo.findByTernNumberAndAcademicYearIdAndSchoolId(activeTerm.getTermNumber() - 1,
                            academicYear.getId(), schoolId).orElse(null)
                    : null;
            if (previousTerm != null) {
                for (Object[] row : scoreRepo.averageScoresForAttentionReport(schoolId, classId, previousTerm.getId(),
                        sectionScopeService.levels(), ACADEMIC_HEURISTIC_EXCLUDED)) {
                    previousAverages.put((String) row[0], new double[] {
                            ((Number) row[1]).doubleValue(),
                            ((Number) row[2]).doubleValue()
                    });
                }
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

            var attendance = AttendanceAttentionCalculator.evaluate(daysByEnrollment.get(enrollment.getId()),
                    rulesByLevel.getOrDefault(levelByClass.get(enrollment.getClazz().getId()), schoolRules),
                    termStart);
            Double attendanceRate = attendance.recentRate();
            // Only a real concern counts here - a "watch" (recent dip, term fine) isn't evidence of a
            // student needing management's attention, matching the class badge.
            boolean lowAttendanceSignal = attendance.level().atLeast(AttendanceAttentionCalculator.Level.NEEDS_ATTENTION);

            // Same judgement as the class view (AcademicAttentionCalculator): below the pass mark, unless
            // the student is already improving on last term. A "watch" isn't counted as a signal here.
            var current = academicAverages.get(enrollment.getId());
            var previous = previousAverages.get(enrollment.getId());
            var academic = AcademicAttentionCalculator.evaluate(
                    current != null ? Math.round(current[0] * 10.0) / 10.0 : null,
                    current != null ? (long) current[1] : 0,
                    previous != null ? Math.round(previous[0] * 10.0) / 10.0 : null,
                    previous != null ? (long) previous[1] : 0,
                    passMark);
            Double academicAverage = academic.average();
            boolean lowAcademicSignal = academic.level().atLeast(AttendanceAttentionCalculator.Level.NEEDS_ATTENTION);

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
