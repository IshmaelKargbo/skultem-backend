package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PerformanceTrendPointDTO;
import com.moriba.skultem.application.dto.StudentPerformanceTrendDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.service.AcademicPerformanceCalculator;
import com.moriba.skultem.domain.service.PerformanceTrendCalculator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Performance Trends drill-down for one student within one term - the chronological series is
// ordered by assessment position (CA1, CA2, Mid-Term, ...) and restricted to approved data, same
// as GetClassAcademicPerformanceUseCase.
@Service
@Transactional
@RequiredArgsConstructor
public class GetStudentPerformanceTrendUseCase {

    private final AssessmentScoreRepository scoreRepo;
    private final EnrollmentRepository enrollmentRepo;

    public StudentPerformanceTrendDTO execute(String schoolId, String enrollmentId, String termId) {
        enrollmentRepo.findByIdAndSchoolId(enrollmentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found"));

        var rows = scoreRepo.assessmentTrendByEnrollmentAndTerm(schoolId, enrollmentId, termId,
                GetClassAcademicPerformanceUseCase.APPROVED_STATUSES);

        List<PerformanceTrendPointDTO> points = rows.stream()
                .map(row -> new PerformanceTrendPointDTO(
                        (String) row[1],
                        AcademicPerformanceCalculator.round1Dp(((Number) row[3]).doubleValue())))
                .toList();

        var trend = PerformanceTrendCalculator
                .evaluate(points.stream().map(PerformanceTrendPointDTO::score).toList());

        return new StudentPerformanceTrendDTO(trend, points);
    }
}
