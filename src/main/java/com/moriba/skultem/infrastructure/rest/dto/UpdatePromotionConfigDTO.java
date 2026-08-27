package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Min;

public record UpdatePromotionConfigDTO(
        Integer minPassMark,
        @Min(value = 1, message = "Max repeat count must be at least 1") int maxRepeatCount,
        boolean requireApproval,
        boolean requireRemarkForPromote) {
}
