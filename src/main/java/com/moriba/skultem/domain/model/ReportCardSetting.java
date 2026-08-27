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
    private boolean showGradeScale;

    public ReportCardSetting(String id, String schoolId, String headerColor, String logoUrl, String footerNote,
            boolean showAttendance, boolean showRemarks, boolean showPosition, boolean showSignatures,
            boolean showGradeScale, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.headerColor = headerColor;
        this.logoUrl = logoUrl;
        this.footerNote = footerNote;
        this.showAttendance = showAttendance;
        this.showRemarks = showRemarks;
        this.showPosition = showPosition;
        this.showSignatures = showSignatures;
        this.showGradeScale = showGradeScale;
        touch(updatedAt);
    }

    public static ReportCardSetting create(String id, String schoolId, String headerColor, String logoUrl,
            String footerNote, boolean showAttendance, boolean showRemarks, boolean showPosition,
            boolean showSignatures, boolean showGradeScale) {
        Instant now = Instant.now();
        return new ReportCardSetting(id, schoolId, headerColor, logoUrl, footerNote, showAttendance, showRemarks,
                showPosition, showSignatures, showGradeScale, now, now);
    }

    public void update(String headerColor, String logoUrl, String footerNote, boolean showAttendance,
            boolean showRemarks, boolean showPosition, boolean showSignatures, boolean showGradeScale) {
        this.headerColor = headerColor;
        this.logoUrl = logoUrl;
        this.footerNote = footerNote;
        this.showAttendance = showAttendance;
        this.showRemarks = showRemarks;
        this.showPosition = showPosition;
        this.showSignatures = showSignatures;
        this.showGradeScale = showGradeScale;
        touch(Instant.now());
    }
}
