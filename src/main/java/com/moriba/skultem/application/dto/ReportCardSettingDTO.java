package com.moriba.skultem.application.dto;

public record ReportCardSettingDTO(
        String headerColor,
        String logoUrl,
        String footerNote,
        boolean showAttendance,
        boolean showRemarks,
        boolean showPosition,
        boolean showSignatures,
        boolean showGradeScale) {
}
