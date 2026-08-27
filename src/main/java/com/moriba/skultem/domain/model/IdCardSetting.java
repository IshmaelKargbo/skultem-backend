package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

// One card design per school (see [[id-cards-rebuild]] scope decision) — no
// multi-template gallery. `fields` is a raw JSON array string (field key/
// label/icon/slot/enabled) so the backend doesn't need to model every field
// shape; the frontend owns that structure.
@Getter
public class IdCardSetting extends AggregateRoot<String> {

    private String schoolId;
    private String layout;
    private String profileShape;
    private String headerColor;
    private String footerColor;
    private String headerTextColor;
    private String primaryTextColor;
    private int widthMm;
    private int heightMm;
    private String bgImageUrl;
    private int bgOpacity;
    private String schoolName;
    private String schoolAddress;
    private String principalName;
    private String fields;
    private int validityYears;

    public IdCardSetting(String id, String schoolId, String layout, String profileShape, String headerColor,
            String footerColor, String headerTextColor, String primaryTextColor, int widthMm, int heightMm,
            String bgImageUrl, int bgOpacity, String schoolName, String schoolAddress, String principalName,
            String fields, int validityYears, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.layout = layout;
        this.profileShape = profileShape;
        this.headerColor = headerColor;
        this.footerColor = footerColor;
        this.headerTextColor = headerTextColor;
        this.primaryTextColor = primaryTextColor;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
        this.bgImageUrl = bgImageUrl;
        this.bgOpacity = bgOpacity;
        this.schoolName = schoolName;
        this.schoolAddress = schoolAddress;
        this.principalName = principalName;
        this.fields = fields;
        this.validityYears = validityYears > 0 ? validityYears : 1;
        touch(updatedAt);
    }

    public static IdCardSetting create(String id, String schoolId, String layout, String profileShape,
            String headerColor, String footerColor, String headerTextColor, String primaryTextColor, int widthMm,
            int heightMm, String bgImageUrl, int bgOpacity, String schoolName, String schoolAddress,
            String principalName, String fields, int validityYears) {
        Instant now = Instant.now();
        return new IdCardSetting(id, schoolId, layout, profileShape, headerColor, footerColor, headerTextColor,
                primaryTextColor, widthMm, heightMm, bgImageUrl, bgOpacity, schoolName, schoolAddress, principalName,
                fields, validityYears, now, now);
    }

    public void update(String layout, String profileShape, String headerColor, String footerColor,
            String headerTextColor, String primaryTextColor, int widthMm, int heightMm, String bgImageUrl,
            int bgOpacity, String schoolName, String schoolAddress, String principalName, String fields,
            int validityYears) {
        this.layout = layout;
        this.profileShape = profileShape;
        this.headerColor = headerColor;
        this.footerColor = footerColor;
        this.headerTextColor = headerTextColor;
        this.primaryTextColor = primaryTextColor;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
        this.bgImageUrl = bgImageUrl;
        this.bgOpacity = bgOpacity;
        this.schoolName = schoolName;
        this.schoolAddress = schoolAddress;
        this.principalName = principalName;
        this.fields = fields;
        this.validityYears = validityYears > 0 ? validityYears : 1;
        touch(Instant.now());
    }
}
