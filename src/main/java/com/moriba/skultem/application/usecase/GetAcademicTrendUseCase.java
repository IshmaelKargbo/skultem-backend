package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AcademicTrendDTO;
import com.moriba.skultem.application.dto.PerformanceTrendPointDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.service.AcademicPerformanceCalculator;
import com.moriba.skultem.domain.service.PerformanceTrendCalculator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Class/school-level Academic Trend - the aggregate counterpart to
// GetStudentPerformanceTrendUseCase (which is per-student). Ordered by assessment position within
// one term, approved data only, classId/subjectId nullable (whole school / every subject).
@Service
@Transactional
@RequiredArgsConstructor
public class GetAcademicTrendUseCase {

    private final AssessmentScoreRepository scoreRepo;
    private final TermRepository termRepo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public AcademicTrendDTO execute(String schoolId, String classId, String subjectId, String academicYearId,
            String termId) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
        var term = resolveTerm(schoolId, academicYear.getId(), termId);

        var rows = scoreRepo.assessmentAverageTrendForReport(schoolId, classId, term.getId(), subjectId,
                GetClassAcademicPerformanceUseCase.APPROVED_STATUSES);

        List<PerformanceTrendPointDTO> points = rows.stream()
                .map(row -> new PerformanceTrendPointDTO(
                        (String) row[1],
                        AcademicPerformanceCalculator.round1Dp(((Number) row[3]).doubleValue())))
                .toList();

        var trend = PerformanceTrendCalculator
                .evaluate(points.stream().map(PerformanceTrendPointDTO::score).toList());

        return new AcademicTrendDTO(trend, points);
    }

    private Term resolveTerm(String schoolId, String academicYearId, String termId) {
        if (termId != null && !termId.isBlank()) {
            return termRepo.findByIdAndAcademicYearIdAndSchoolId(termId, academicYearId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Term not found"));
        }

        return termRepo.findActiveBySchoolAndAcademicYear(schoolId, academicYearId)
                .orElseThrow(() -> new NotFoundException("Active term not found"));
    }
}
