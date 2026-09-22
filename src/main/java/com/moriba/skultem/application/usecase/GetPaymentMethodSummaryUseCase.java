package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PaymentMethodSummaryDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PaymentMethodRowMapper;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Collected money broken down by payment method, for one academic year, one term, or an explicit
 * date range - whichever the caller narrows to. An explicit date range takes precedence; otherwise
 * the term's (or, with no term, the academic year's) own date span is used.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GetPaymentMethodSummaryUseCase {

    private final PaymentRepository paymentRepo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final TermRepository termRepo;

    public PaymentMethodSummaryDTO execute(String schoolId, String academicYearId, String termId, LocalDate from,
            LocalDate to) {
        LocalDate rangeFrom;
        LocalDate rangeTo;

        if (from != null || to != null) {
            rangeFrom = from != null ? from : LocalDate.EPOCH;
            rangeTo = to != null ? to : LocalDate.now();
        } else if (termId != null && !termId.isBlank()) {
            var term = termRepo.findByIdAndSchoolId(termId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Term not found"));
            rangeFrom = term.getStartDate();
            rangeTo = term.getEndDate();
        } else {
            var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
            rangeFrom = academicYear.getStartDate();
            rangeTo = academicYear.getEndDate();
        }

        Instant start = rangeFrom.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = rangeTo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        var byMethod = paymentRepo.sumSchoolPaymentsByMethodAndDateRange(schoolId, start, end);
        var totals = PaymentMethodRowMapper.totals(byMethod);
        var methodRows = PaymentMethodRowMapper.toRows(byMethod, totals.amount());

        return new PaymentMethodSummaryDTO(totals.amount(), methodRows);
    }
}
