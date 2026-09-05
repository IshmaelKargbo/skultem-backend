package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassAttentionDTO;
import com.moriba.skultem.application.dto.StudentAttentionDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Flags students in a class as "needing attention" - either their attendance rate over the last 30
// days is below 75%, or their average assessment score so far this term is below the class's pass
// mark (same passMark GenerateReportCardsUseCase uses, defaulting to 50 when no template is set).
// Backs the /classes list's "Needs Attention" badge and the per-student breakdown on a class's own
// page - both call this same method, so the criteria can't drift between the two views.
@Service
@Transactional
@RequiredArgsConstructor
public class ComputeClassAttentionUseCase {

    private static final double ATTENDANCE_THRESHOLD = 75.0;
    private static final int ATTENDANCE_WINDOW_DAYS = 30;
    private static final int DEFAULT_PASS_MARK = 50;

    private final ClassRepository classRepo;
    private final TermRepository termRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final AttendanceRepository attendanceRepo;
    private final AssessmentScoreRepository scoreRepo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public ClassAttentionDTO execute(String schoolId, String classId, String academicYearId) {
        var clazz = classRepo.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        int passMark = clazz.getTemplate() != null ? clazz.getTemplate().getPassMark() : DEFAULT_PASS_MARK;

        // No active term yet (e.g. a brand-new academic year) just means no academic data to flag
        // on - attendance can still be checked.
        var activeTerm = termRepo.findActiveBySchoolAndAcademicYear(schoolId, academicYear.getId()).orElse(null);

        var enrollments = enrollmentRepo
                .findAllByClassAndAcademicAndSchoolId(classId, academicYear.getId(), schoolId, Pageable.unpaged())
                .getContent();

        Map<String, double[]> attendanceCounts = new HashMap<>(); // enrollmentId -> [present, total]
        LocalDate since = LocalDate.now().minusDays(ATTENDANCE_WINDOW_DAYS);
        for (Object[] row : attendanceRepo.attendanceCountsByClassSince(schoolId, classId, academicYear.getId(),
                since)) {
            attendanceCounts.put((String) row[0], new double[] {
                    ((Number) row[1]).doubleValue(),
                    ((Number) row[2]).doubleValue()
            });
        }

        Map<String, double[]> academicAverages = new HashMap<>(); // enrollmentId -> [average, count]
        if (activeTerm != null) {
            for (Object[] row : scoreRepo.averageScoresByClassAndTerm(schoolId, classId, activeTerm.getId(),
                    ClassSubjectAssessmentLifeCycle.Status.DRAFT)) {
                academicAverages.put((String) row[0], new double[] {
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).doubleValue()
                });
            }
        }

        var flagged = new ArrayList<StudentAttentionDTO>();

        for (var enrollment : enrollments) {
            var attendance = attendanceCounts.get(enrollment.getId());
            Double attendanceRate = (attendance != null && attendance[1] > 0)
                    ? Math.round((attendance[0] / attendance[1]) * 1000.0) / 10.0
                    : null;
            boolean attendanceFlag = attendanceRate != null && attendanceRate < ATTENDANCE_THRESHOLD;

            var academic = academicAverages.get(enrollment.getId());
            Double average = (academic != null && academic[1] > 0)
                    ? Math.round(academic[0] * 10.0) / 10.0
                    : null;
            boolean academicFlag = average != null && average < passMark;

            if (!attendanceFlag && !academicFlag) {
                continue;
            }

            var student = enrollment.getStudent();
            flagged.add(new StudentAttentionDTO(
                    student.getId(),
                    enrollment.getId(),
                    student.getGivenNames(),
                    student.getFamilyName(),
                    student.getPhoto(),
                    attendanceRate,
                    average,
                    attendanceFlag,
                    academicFlag));
        }

        return new ClassAttentionDTO(enrollments.size(), flagged.size(), flagged);
    }
}
