package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdatePlatformFeeSettingDTO(
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        // BY_TERM (one part per term - the default) or FULL; null keeps the current plan.
        @jakarta.validation.constraints.Pattern(regexp = "FULL|BY_TERM", message = "Plan must be FULL or BY_TERM")
        String installmentMode,

        // Percent per term (e.g. [50, 30, 20]); empty = equal parts; null keeps the current split.
        java.util.List<Integer> termShares) {
}
