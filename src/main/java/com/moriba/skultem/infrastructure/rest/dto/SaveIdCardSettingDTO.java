package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveIdCardSettingDTO(
        @NotBlank(message = "Layout is required") String layout,
        @NotBlank(message = "Profile shape is required") String profileShape,
        @NotBlank(message = "Header color is required") String headerColor,
        @NotBlank(message = "Footer color is required") String footerColor,
        @NotBlank(message = "Header text color is required") String headerTextColor,
        @NotBlank(message = "Primary text color is required") String primaryTextColor,
        int widthMm,
        int heightMm,
        String bgImageUrl,
        int bgOpacity,
        String schoolName,
        String schoolAddress,
        String principalName,
        @NotBlank(message = "Fields are required") String fields,
        int validityYears) {
}
