package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

// One payment receipt design per school, same "single active config" pattern as
// ReportCardSetting/IdCardSetting - no multi-template gallery.
@Getter
public class ReceiptSetting extends AggregateRoot<String> {

    private String schoolId;
    private String accentColor;
    private String logoUrl;
    private String footerNote;
    private boolean showWatermark;
    private boolean showAmountInWords;

    public ReceiptSetting(String id, String schoolId, String accentColor, String logoUrl, String footerNote,
            boolean showWatermark, boolean showAmountInWords, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.accentColor = accentColor;
        this.logoUrl = logoUrl;
        this.footerNote = footerNote;
        this.showWatermark = showWatermark;
        this.showAmountInWords = showAmountInWords;
        touch(updatedAt);
    }

    public static ReceiptSetting create(String id, String schoolId, String accentColor, String logoUrl,
            String footerNote, boolean showWatermark, boolean showAmountInWords) {
        Instant now = Instant.now();
        return new ReceiptSetting(id, schoolId, accentColor, logoUrl, footerNote, showWatermark, showAmountInWords,
                now, now);
    }

    public void update(String accentColor, String logoUrl, String footerNote, boolean showWatermark,
            boolean showAmountInWords) {
        this.accentColor = accentColor;
        this.logoUrl = logoUrl;
        this.footerNote = footerNote;
        this.showWatermark = showWatermark;
        this.showAmountInWords = showAmountInWords;
        touch(Instant.now());
    }
}
