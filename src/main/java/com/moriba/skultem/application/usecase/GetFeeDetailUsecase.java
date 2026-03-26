package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    public FeeDetail execute(String schoolId, String studentId) {

        // Prepare filters
        var filters = new ArrayList<Filter>();
        filters.add(new Filter("student.id", FilterOperator.EQUALS, "select", studentId, null, null));
        var request = new ReportBuilderDTO(schoolId, "fees", filters);

        // Fetch fees
        List<StudentFeeDTO> fees = feeReportUseCase.execute(request, 0, 0).getContent();

        // Initialize totals and fee status list
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;

        for (StudentFeeDTO fee : fees) {
            BigDecimal feeAmount = fee.amount() != null ? fee.amount() : BigDecimal.ZERO;
            BigDecimal amountPaid = fee.amountPaid() != null ? fee.amountPaid() : BigDecimal.ZERO;
            BigDecimal outstanding = fee.outstanding() != null ? fee.outstanding() : BigDecimal.ZERO;

            // Add to totals
            totalAmount = totalAmount.add(feeAmount);
            totalPaid = totalPaid.add(amountPaid);
            totalOutstanding = totalOutstanding.add(outstanding);
        }

        // Determine overall status
        String overallStatus;
        if (totalPaid.compareTo(totalAmount) >= 0) {
            overallStatus = "Paid";
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            overallStatus = "Partial";
        } else {
            overallStatus = "Pending";
        }

        // Return full fee detail
        return new FeeDetail(totalAmount, totalPaid, totalOutstanding, overallStatus);
    }
}