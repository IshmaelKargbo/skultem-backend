package com.moriba.skultem.application.dto;

import java.util.List;

import com.moriba.skultem.domain.vo.RemarkBand;

public record ReportCardSettingDTO(
        String headerColor,
        String logoUrl,
        String footerNote,
        boolean showAttendance,
        boolean showRemarks,
        boolean showPosition,
        boolean showSignatures,
        boolean showTeacherSignature,
        boolean showPrincipalSignature,
        boolean showGradeScale,
        List<RemarkBand> remarkScale) {
}
