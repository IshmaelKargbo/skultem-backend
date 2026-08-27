package com.moriba.skultem.application.dto;

public record IdCardSettingDTO(
        String layout,
        String profileShape,
        String headerColor,
        String footerColor,
        String headerTextColor,
        String primaryTextColor,
        int widthMm,
        int heightMm,
        String bgImageUrl,
        int bgOpacity,
        String schoolName,
        String schoolAddress,
        String principalName,
        String fields,
        int validityYears) {
}
