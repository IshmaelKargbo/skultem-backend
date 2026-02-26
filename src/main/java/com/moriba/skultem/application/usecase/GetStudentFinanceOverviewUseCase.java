package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.OutstandingBalanceDTO;
import com.moriba.skultem.application.dto.PaymentDTO;
import com.moriba.skultem.application.dto.StudentFinanceOverviewDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetStudentFinanceOverviewUseCase {
    private final GetStudentUseCase getStudentUseCase;
    private final CountStudentFeesUseCase countStudentFeesUseCase;
    private final FinanceReportUseCase financeReportUseCase;

    public StudentFinanceOverviewDTO execute(String schoolId, String studentId, int recentPaymentSize) {
        var student = getStudentUseCase.execute(studentId, schoolId);
        Double assignedFeeTotal = countStudentFeesUseCase.execute(schoolId, studentId);
        if (assignedFeeTotal == null) {
            assignedFeeTotal = 0.0;
        }

        List<OutstandingBalanceDTO> fees = financeReportUseCase.outstandingForStudent(schoolId, studentId);
        List<PaymentDTO> recentPayments = financeReportUseCase.paymentHistory(
                schoolId,
                studentId,
                0,
                recentPaymentSize).getContent();

        BigDecimal totalPaid = fees.stream()
                .map(OutstandingBalanceDTO::paid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = fees.stream()
                .map(OutstandingBalanceDTO::discount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = fees.stream()
                .map(OutstandingBalanceDTO::outstanding)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StudentFinanceOverviewDTO(
                student,
                assignedFeeTotal,
                totalPaid,
                totalDiscount,
                totalOutstanding,
                fees,
                recentPayments);
    }
}
