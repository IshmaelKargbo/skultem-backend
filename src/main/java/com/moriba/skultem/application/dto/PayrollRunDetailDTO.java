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
        long totalCount,
        // What this run put through the school's transactions (null while still a draft - nothing
        // is recorded until it is generated), and how many correcting entries adjusted it.
        BigDecimal transactionsAmount,
        long transactionCorrections) {
}
