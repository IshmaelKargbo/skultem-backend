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
        BigDecimal assignedFeeTotal = countStudentFeesUseCase.execute(schoolId, studentId);
        if (assignedFeeTotal == null) {
            assignedFeeTotal = BigDecimal.ZERO;
        }

        List<OutstandingBalanceDTO> fees = financeReportUseCase.outstandingForStudent(schoolId, studentId);
        List<PaymentDTO> recentPayments = financeReportUseCase.paymentHistory(
                schoolId,
                studentId,
                0,
                recentPaymentSize).getContent();

        BigDecimal totalPaid = fees.stream()
                .map(arg0 -> arg0.paid())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        BigDecimal totalDiscount = fees.stream()
                .map(a -> a.discount())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        BigDecimal totalOutstanding = fees.stream()
                .map(a -> a.outstanding())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

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
