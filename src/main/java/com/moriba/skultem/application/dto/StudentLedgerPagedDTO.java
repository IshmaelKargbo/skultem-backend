package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudentLedgerPagedDTO(
        List<StudentLedgerDTO> records,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal finalBalance,
        long totalElements,
        int totalPages,
        int page,
        int size) {
}
