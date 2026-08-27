package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveReportCardSettingDTO(
        @NotBlank(message = "Header color is required") String headerColor,
        String logoUrl,
        String footerNote,
        boolean showAttendance,
        boolean showRemarks,
        boolean showPosition,
        boolean showSignatures,
        boolean showGradeScale) {
}
