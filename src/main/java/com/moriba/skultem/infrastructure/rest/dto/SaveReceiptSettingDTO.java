package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveReceiptSettingDTO(
        @NotBlank(message = "Accent color is required") String accentColor,
        String logoUrl,
        String footerNote,
        boolean showWatermark,
        boolean showAmountInWords) {
}
