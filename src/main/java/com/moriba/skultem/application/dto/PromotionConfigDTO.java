package com.moriba.skultem.application.dto;

public record PromotionConfigDTO(
        Integer minPassMark,
        int maxRepeatCount,
        boolean requireApproval,
        boolean requireRemarkForPromote) {
}
