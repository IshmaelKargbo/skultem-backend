package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeDashboardSummaryDTO;
import com.moriba.skultem.application.dto.SchoolFeeRowDTO;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.service.FeeCollectionCalculator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** The school's current financial position for one academic year (optionally one term). */
@Service
@Transactional
@RequiredArgsConstructor
public class GetFeeDashboardSummaryUseCase {

    private final LoadSchoolFeeRowsUseCase loadSchoolFeeRowsUseCase;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final TermRepository termRepo;

    public FeeDashboardSummaryDTO execute(String schoolId, String academicYearId, String termId) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
        String termName = (termId != null && !termId.isBlank())
                ? termRepo.findByIdAndSchoolId(termId, schoolId).map(t -> t.getName()).orElse(null)
                : null;

        var rows = loadSchoolFeeRowsUseCase.execute(schoolId, academicYear.getId(), termId);

        BigDecimal totalExpected = rows.stream().map(SchoolFeeRowDTO::expected).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCollected = rows.stream().map(SchoolFeeRowDTO::paid).reduce(BigDecimal.ZERO, BigDecimal::add);
        // Summed per-row balance (each already floored at zero), not expected-minus-collected - an
        // overpayment on one fee must never offset a genuine balance on another.
        BigDecimal totalOutstanding = rows.stream().map(SchoolFeeRowDTO::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        double collectionRate = FeeCollectionCalculator.rate(totalCollected, totalExpected);

        Map<String, List<SchoolFeeRowDTO>> byStudent = rows.stream()
                .collect(Collectors.groupingBy(SchoolFeeRowDTO::studentId));

        int fullyPaid = 0, withBalance = 0, noPayment = 0;
        for (var studentRows : byStudent.values()) {
            BigDecimal paid = studentRows.stream().map(SchoolFeeRowDTO::paid).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal balance = studentRows.stream().map(SchoolFeeRowDTO::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                fullyPaid++;
            } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                withBalance++;
            } else {
                noPayment++;
            }
        }

        return new FeeDashboardSummaryDTO(academicYear.getId(), academicYear.getName(), termId, termName,
                totalExpected, totalCollected, totalOutstanding, collectionRate, byStudent.size(), fullyPaid,
                withBalance, noPayment);
    }
}
