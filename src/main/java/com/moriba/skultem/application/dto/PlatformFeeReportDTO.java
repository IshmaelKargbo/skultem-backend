package com.moriba.skultem.application.dto;

/**
 * A school's platform fee for one academic year: what was charged to its students, what they've
 * paid, and what's left. The platform fee is Skultem's, collected by the school on its behalf, and
 * is kept out of the school's own student ledger.
 */
public record PlatformFeeReportDTO(long expected, long collected, long outstanding) {
}
