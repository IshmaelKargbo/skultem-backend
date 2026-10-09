package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveTimetableSettingDTO(
        @Size(max = 80, message = "Title must be 80 characters or less") String title,
        @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Accent color must be a hex color like #2f5f96") String accentColor,
        @Pattern(regexp = "LANDSCAPE|PORTRAIT", message = "Orientation must be LANDSCAPE or PORTRAIT") String orientation,
        boolean showLogo,
        boolean showIcons,
        boolean showTeacher,
        boolean showRoom,
        boolean showPeriodTimes,
        @Size(max = 255, message = "Footer note must be 255 characters or less") String footerNote) {
}
