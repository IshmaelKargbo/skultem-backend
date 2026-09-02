package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record SavePayslipSettingDTO(
        @NotBlank(message = "Accent color is required") String accentColor,
        String logoUrl,
        String footerNote,
        boolean showWatermark,
        boolean showAmountInWords) {
}
