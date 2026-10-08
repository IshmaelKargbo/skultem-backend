package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import com.moriba.skultem.domain.vo.RemarkBand;

import jakarta.validation.constraints.NotBlank;

public record SaveReportCardSettingDTO(
        @NotBlank(message = "Header color is required") String headerColor,
        String logoUrl,
        String footerNote,
        boolean showAttendance,
        boolean showRemarks,
        boolean showPosition,
        boolean showTeacherSignature,
        boolean showPrincipalSignature,
        boolean showGradeScale,
        List<RemarkBand> remarkScale) {
}
