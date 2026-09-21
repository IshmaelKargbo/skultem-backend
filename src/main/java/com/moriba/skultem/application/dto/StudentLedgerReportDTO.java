package com.moriba.skultem.application.dto;

/** The student ledger's totals for the school's own fees - the platform fee is reported separately (see PlatformFeeReportDTO). */
public record StudentLedgerReportDTO(long totalDebit, long totalCredit, long netBalance) {
}
