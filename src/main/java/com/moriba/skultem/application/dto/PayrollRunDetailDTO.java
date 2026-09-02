package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record PayrollRunDetailDTO(
        PayrollRunDTO run,
        List<PayslipDTO> payslips,
        BigDecimal grossTotal,
        BigDecimal deductionTotal,
        BigDecimal netTotal,
        long includedCount,
        long totalCount) {
}
