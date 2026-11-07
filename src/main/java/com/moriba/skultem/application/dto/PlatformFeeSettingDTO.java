package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

// installmentMode: BY_TERM (default, one part per term) or FULL; termShares: percent per term, empty = equal parts.
public record PlatformFeeSettingDTO(String schoolId, BigDecimal amount, Instant updatedAt, String installmentMode,
        java.util.List<Integer> termShares) {

    public static PlatformFeeSettingDTO of(String schoolId, com.moriba.skultem.domain.model.PlatformFeeSetting s) {
        if (s == null) {
            return new PlatformFeeSettingDTO(schoolId, null, null, "BY_TERM", java.util.List.of());
        }
        return new PlatformFeeSettingDTO(schoolId, s.getAmount(), s.getUpdatedAt(), s.getInstallmentMode().name(),
                s.getTermShares());
    }
}
