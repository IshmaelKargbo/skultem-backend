package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

// One report card design per school, same "single active config" pattern as
// IdCardSetting - no multi-template gallery. Which sections print is a set of
// booleans rather than a JSON blob because, unlike ID card fields, the report
// card's section list is fixed and small; nothing here needs the frontend to
// own an open-ended shape.
@Getter
public class ReportCardSetting extends AggregateRoot<String> {

    private String schoolId;
    private String headerColor;
    private String logoUrl;
    private String footerNote;
    private boolean showAttendance;
    private boolean showRemarks;
    private boolean showPosition;
    private boolean showSignatures;
    private boolean showTeacherSignature;
    private boolean showPrincipalSignature;
    private boolean showGradeScale;
    // JSON array of RemarkBand - null/empty until the school configures one.
    private String remarkScale;

    public ReportCardSetting(String id, String schoolId, String headerColor, String logoUrl, String footerNote,
            boolean showAttendance, boolean showRemarks, boolean showPosition, boolean showSignatures,
            boolean showTeacherSignature, boolean showPrincipalSignature, boolean showGradeScale, String remarkScale,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.headerColor = headerColor;
        this.logoUrl = logoUrl;
        this.footerNote = footerNote;
        this.showAttendance = showAttendance;
        this.showRemarks = showRemarks;
        this.showPosition = showPosition;
        this.showSignatures = showSignatures;
        this.showTeacherSignature = showTeacherSignature;
        this.showPrincipalSignature = showPrincipalSignature;
        this.showGradeScale = showGradeScale;
        this.remarkScale = remarkScale;
        touch(updatedAt);
    }

    public static ReportCardSetting create(String id, String schoolId, String headerColor, String logoUrl,
            String footerNote, boolean showAttendance, boolean showRemarks, boolean showPosition,
            boolean showTeacherSignature, boolean showPrincipalSignature, boolean showGradeScale,
            String remarkScale) {
        Instant now = Instant.now();
        return new ReportCardSetting(id, schoolId, headerColor, logoUrl, footerNote, showAttendance, showRemarks,
                showPosition, showTeacherSignature || showPrincipalSignature, showTeacherSignature,
                showPrincipalSignature, showGradeScale, remarkScale, now, now);
    }

    public void update(String headerColor, String logoUrl, String footerNote, boolean showAttendance,
            boolean showRemarks, boolean showPosition, boolean showTeacherSignature, boolean showPrincipalSignature,
            boolean showGradeScale, String remarkScale) {
        this.headerColor = headerColor;
        this.logoUrl = logoUrl;
        this.footerNote = footerNote;
        this.showAttendance = showAttendance;
        this.showRemarks = showRemarks;
        this.showPosition = showPosition;
        this.showTeacherSignature = showTeacherSignature;
        this.showPrincipalSignature = showPrincipalSignature;
        this.showSignatures = showTeacherSignature || showPrincipalSignature;
        this.showGradeScale = showGradeScale;
        this.remarkScale = remarkScale;
        touch(Instant.now());
    }
}
