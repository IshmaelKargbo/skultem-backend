package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.DailyCollectionReportDTO;
import com.moriba.skultem.application.mapper.PaymentMethodRowMapper;
import com.moriba.skultem.domain.repository.PaymentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** What was collected on a day, or across a date range - broken down by payment method. */
@Service
@Transactional
@RequiredArgsConstructor
public class GetDailyCollectionReportUseCase {

    private final PaymentRepository paymentRepo;

    public DailyCollectionReportDTO execute(String schoolId, LocalDate from, LocalDate to) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now();
        LocalDate resolvedTo = to != null ? to : resolvedFrom;

        Instant start = resolvedFrom.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = resolvedTo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        var byMethod = paymentRepo.sumSchoolPaymentsByMethodAndDateRange(schoolId, start, end);
        var totals = PaymentMethodRowMapper.totals(byMethod);
        var methodRows = PaymentMethodRowMapper.toRows(byMethod, totals.amount());

        return new DailyCollectionReportDTO(resolvedFrom, resolvedTo, totals.amount(), totals.count(), methodRows);
    }
}
