package com.moriba.skultem.application.dto;

public record ReceiptSettingDTO(
        String accentColor,
        String logoUrl,
        String footerNote,
        boolean showWatermark,
        boolean showAmountInWords) {
}
