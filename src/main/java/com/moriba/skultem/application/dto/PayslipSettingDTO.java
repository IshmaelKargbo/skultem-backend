package com.moriba.skultem.application.dto;

public record PayslipSettingDTO(
        String accentColor,
        String logoUrl,
        String footerNote,
        boolean showWatermark,
        boolean showAmountInWords) {
}
