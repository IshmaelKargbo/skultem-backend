package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassAttentionDTO;
import com.moriba.skultem.application.dto.StudentAttentionDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.AttendanceRulesResolver;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.service.AcademicAttentionCalculator;
import com.moriba.skultem.domain.service.AttendanceAttentionCalculator;
import com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Flags students in a class as "needing attention" - either their attendance is a concern (see
// AttendanceAttentionCalculator: the last ~20 recorded school days judged against the school's
// threshold, with term-to-date, trend and absence streak for context, so a student who improves
// drops out) or their average assessment score so far this term is below the class's pass mark
// (same passMark GenerateReportCardsUseCase uses, defaulting to 50 when no template is set - see
// AcademicAttentionCalculator, which also weighs the change since last term). Backs the /classes list's "Needs Attention" badge and the
// per-student breakdown on a class's own page - both call this same method, so the criteria can't
// drift between the two views.
@Service
@Transactional
@RequiredArgsConstructor
public class ComputeClassAttentionUseCase {

    // How far back attendance rows are fetched - comfortably more than two 20-day windows of school
    // days, even across a break. Term to date is cut from the same rows.
    private static final int MIN_LOOKBACK_DAYS = 90;
    private static final int DEFAULT_PASS_MARK = 50;

    private final ClassRepository classRepo;
    private final TermRepository termRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final AttendanceRepository attendanceRepo;
    private final AssessmentScoreRepository scoreRepo;
    private final SchoolRepository schoolRepo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final AttendanceRulesResolver attendanceRulesResolver;

    // Calendar days of attendance to fetch: two windows of school days (~5 per 7 calendar days), with
    // slack for breaks - never less than MIN_LOOKBACK_DAYS so term to date is covered too.
    private static int lookbackDays(AttendanceAttentionCalculator.Rules rules) {
        return Math.max(MIN_LOOKBACK_DAYS, rules.windowDays() * 4);
    }

    public ClassAttentionDTO execute(String schoolId, String classId, String academicYearId) {
        var clazz = classRepo.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));
        // This class's section may have its own rules (see AttendanceRulesResolver); else the school's.
        var attendanceRules = attendanceRulesResolver.forLevel(school, clazz.getLevel());

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        int passMark = clazz.getTemplate() != null ? clazz.getTemplate().getPassMark() : DEFAULT_PASS_MARK;

        // No active term yet (e.g. a brand-new academic year) just means no academic data to flag
        // on - attendance can still be checked.
        var activeTerm = termRepo.findActiveBySchoolAndAcademicYear(schoolId, academicYear.getId()).orElse(null);

        var enrollments = enrollmentRepo
                .findAllByClassAndAcademicAndSchoolId(classId, academicYear.getId(), schoolId, Pageable.unpaged())
                .getContent();

        LocalDate today = LocalDate.now();
        LocalDate since = today.minusDays(lookbackDays(attendanceRules));
        LocalDate termStart = activeTerm != null ? activeTerm.getStartDate() : null;
        if (termStart != null && termStart.isBefore(since)) {
            since = termStart;
        }
        var daysByEnrollment = AttendanceAttentionCalculator.groupByEnrollment(
                attendanceRepo.attendanceDaysSince(schoolId, classId, academicYear.getId(), since));

        var excludedStatuses = List.of(ClassSubjectAssessmentLifeCycle.Status.DRAFT,
                ClassSubjectAssessmentLifeCycle.Status.LOCKED);
        Map<String, double[]> academicAverages = new HashMap<>(); // enrollmentId -> [average, count]
        Map<String, double[]> previousAverages = new HashMap<>(); // same, for the term before
        if (activeTerm != null) {
            for (Object[] row : scoreRepo.averageScoresByClassAndTerm(schoolId, classId, activeTerm.getId(),
                    excludedStatuses)) {
                academicAverages.put((String) row[0], new double[] {
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).doubleValue()
                });
            }

            // The term before this one in the same academic year (none in term 1) - only there to say
            // whether the student is improving or sliding.
            var previousTerm = activeTerm.getTermNumber() > 1
                    ? termRepo.findByTernNumberAndAcademicYearIdAndSchoolId(activeTerm.getTermNumber() - 1,
                            academicYear.getId(), schoolId).orElse(null)
                    : null;
            if (previousTerm != null) {
                for (Object[] row : scoreRepo.averageScoresByClassAndTerm(schoolId, classId, previousTerm.getId(),
                        excludedStatuses)) {
                    previousAverages.put((String) row[0], new double[] {
                            ((Number) row[1]).doubleValue(),
                            ((Number) row[2]).doubleValue()
                    });
                }
            }
        }

        var flagged = new ArrayList<StudentAttentionDTO>();

        int needsAttentionCount = 0;
        int watchCount = 0;

        for (var enrollment : enrollments) {
            var attendance = AttendanceAttentionCalculator.evaluate(daysByEnrollment.get(enrollment.getId()),
                    attendanceRules, termStart);
            boolean attendanceFlag = attendance.level() != Level.NONE;

            var current = academicAverages.get(enrollment.getId());
            var previous = previousAverages.get(enrollment.getId());
            var academic = AcademicAttentionCalculator.evaluate(
                    current != null ? Math.round(current[0] * 10.0) / 10.0 : null,
                    current != null ? (long) current[1] : 0,
                    previous != null ? Math.round(previous[0] * 10.0) / 10.0 : null,
                    previous != null ? (long) previous[1] : 0,
                    passMark);
            boolean academicFlag = academic.level() != Level.NONE;

            Level severity = attendance.level().atLeast(academic.level()) ? attendance.level() : academic.level();
            if (severity == Level.NONE) {
                continue;
            }

            if (severity == Level.WATCH) {
                watchCount++;
            } else {
                needsAttentionCount++;
            }

            var student = enrollment.getStudent();
            flagged.add(new StudentAttentionDTO(
                    student.getId(),
                    enrollment.getId(),
                    student.getGivenNames(),
                    student.getFamilyName(),
                    student.getPhoto(),
                    attendance.recentRate(),
                    academic.average(),
                    attendanceFlag,
                    academicFlag,
                    attendance.termRate(),
                    attendance.trend(),
                    attendance.absenceStreak(),
                    severity,
                    academic.previousAverage(),
                    academic.trend()));
        }

        return new ClassAttentionDTO(enrollments.size(), needsAttentionCount, watchCount, flagged);
    }
}
