package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeDetail;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.StudentFeeDTO;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.FilterOperator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetFeeDetailUsecase {

    private final FeeReportUseCase feeReportUseCase;
    private final com.moriba.skultem.domain.repository.AcademicYearRepository academicYearRepo;
    private final com.moriba.skultem.domain.repository.EnrollmentRepository enrollmentRepo;
    private final com.moriba.skultem.domain.repository.StudentFeeRepository studentFeeRepo;
    private final com.moriba.skultem.domain.repository.FeeDiscountRepository discountRepo;
    private final com.moriba.skultem.domain.repository.PaymentRepository paymentRepo;

    public FeeDetail execute(String schoolId, String studentId, String termId) {

        // Prepare filters
        var filters = new ArrayList<Filter>();
        filters.add(new Filter("student.id", FilterOperator.EQUALS, "select", studentId, null, null));
        filters.add(new Filter("fee.term.id", FilterOperator.EQUALS, "select", termId, null, null));
        var request = new ReportBuilderDTO(schoolId, "fees", filters);

        // Fetch fees
        List<StudentFeeDTO> fees = feeReportUseCase.execute(request, 0, 0).getContent();

        // Initialize totals and fee status list
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (StudentFeeDTO fee : fees) {
            BigDecimal feeAmount = fee.amount() != null ? fee.amount() : BigDecimal.ZERO;
            BigDecimal amountPaid = fee.amountPaid() != null ? fee.amountPaid() : BigDecimal.ZERO;
            BigDecimal outstanding = fee.outstanding() != null ? fee.outstanding() : BigDecimal.ZERO;

            // Add to totals
            totalAmount = totalAmount.add(feeAmount);
            totalPaid = totalPaid.add(amountPaid);
            totalOutstanding = totalOutstanding.add(outstanding);
            totalDiscount = totalDiscount.add(fee.discount() != null ? fee.discount() : BigDecimal.ZERO);
        }

        // Determine overall status
        String overallStatus;
        BigDecimal payable = totalAmount.subtract(totalDiscount);
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0 && totalPaid.compareTo(payable) >= 0) {
            overallStatus = "Paid";
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            overallStatus = "Partial";
        } else if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            overallStatus = "N/A";
        } else {
            overallStatus = "Pending";
        }

        // Return full fee detail
        return new FeeDetail(totalAmount, totalPaid, totalOutstanding, overallStatus, totalDiscount);
    }

    /**
     * What one student owes this academic year, the same way the Fees reports count it (see
     * LoadSchoolFeeRowsUseCase): only the fees on their enrollment in the active year, never the Skultem
     * platform fee (it isn't the school's money), and net of discounts. {@code total} is what they owe after
     * discounts; {@code discount} is what the discounts took off; {@code balance} is added up fee by fee, each
     * floored at zero, so an overpayment on one fee never hides a balance on another.
     */
    public FeeDetail execute(String schoolId, String studentId) {
        var year = academicYearRepo.findActiveBySchool(schoolId).orElse(null);
        var enrollment = year == null ? null
                : enrollmentRepo.findByStudentAndAcademicYearAndSchoolId(studentId, year.getId(), schoolId).orElse(null);
        if (enrollment == null) {
            return new FeeDetail(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "Paid", BigDecimal.ZERO);
        }

        BigDecimal totalOwed = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (var charge : studentFeeRepo.findBySchoolAndEnrollment(schoolId, enrollment.getId(), Pageable.unpaged())
                .getContent()) {
            var fee = charge.getFee();
            if (fee == null || fee.isSystem()) {
                continue;
            }
            BigDecimal amount = fee.getAmount() == null ? BigDecimal.ZERO : fee.getAmount();
            BigDecimal discount = discountRepo.findBySchoolAndStudentIdAndFeeId(schoolId, studentId, fee.getId())
                    .stream().map(d -> d.computeSavings()).reduce(BigDecimal.ZERO, BigDecimal::add).min(amount);
            BigDecimal paid = paymentRepo.sumPaymentsByStudentAndFee(studentId, fee.getId());
            paid = paid == null ? BigDecimal.ZERO : paid;
            BigDecimal owed = amount.subtract(discount).max(BigDecimal.ZERO);

            totalOwed = totalOwed.add(owed);
            totalDiscount = totalDiscount.add(discount);
            totalPaid = totalPaid.add(paid);
            totalBalance = totalBalance.add(owed.subtract(paid).max(BigDecimal.ZERO));
        }

        String status;
        if (totalBalance.signum() == 0) {
            status = "Paid";
        } else if (totalPaid.signum() > 0) {
            status = "Partial";
        } else {
            status = "Pending";
        }
        return new FeeDetail(totalOwed, totalPaid, totalBalance, status, totalDiscount);
    }
}