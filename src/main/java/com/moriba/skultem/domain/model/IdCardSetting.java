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
    // Staff cards reuse every visual setting above (colour/layout/dimensions/background) - only
    // the field list differs, so it's the one extra property needed rather than a parallel set of
    // colour/layout columns.
    private String staffFields;
    private int validityYears;
    // Logo size as a percentage of the default size (100 = default).
    private int logoSize;
    // Corner roundness of the logo container, 0 (square) to 50 (full circle), as a % of its size.
    private int logoRadius;
    // Principal signature size as a percentage of the default size (100 = default).
    private int signatureSize;
    // Photo container size as a percentage of the default size (100 = default).
    private int photoSize;
    // Padding between the photo container's border and the photo itself, in px.
    private int photoPadding;
    // Photo container border thickness in px (0 = no border).
    private int photoBorder;
    // Border thickness around the whole card in px (0 = no border).
    private int cardBorder;
    // Logo container size as a percentage of the default size (100 = default) and its inner padding in px.
    private int logoBoxSize;
    private int logoPadding;

    public IdCardSetting(String id, String schoolId, String layout, String profileShape, String headerColor,
            String footerColor, String headerTextColor, String primaryTextColor, int widthMm, int heightMm,
            String bgImageUrl, int bgOpacity, String schoolName, String schoolAddress, String principalName,
            String fields, String staffFields, int validityYears, int logoSize, int logoRadius, int signatureSize, int photoSize, int photoPadding, int photoBorder, int cardBorder, int logoBoxSize, int logoPadding, Instant createdAt, Instant updatedAt) {
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
        this.staffFields = staffFields;
        this.validityYears = validityYears > 0 ? validityYears : 1;
        this.logoSize = logoSize > 0 ? logoSize : 100;
        this.logoRadius = Math.max(0, Math.min(50, logoRadius));
        this.signatureSize = signatureSize > 0 ? signatureSize : 100;
        this.photoSize = photoSize > 0 ? photoSize : 100;
        this.photoPadding = Math.max(0, Math.min(24, photoPadding));
        this.photoBorder = Math.max(0, Math.min(12, photoBorder));
        this.cardBorder = Math.max(0, Math.min(12, cardBorder));
        this.logoBoxSize = logoBoxSize > 0 ? logoBoxSize : 100;
        this.logoPadding = Math.max(0, Math.min(24, logoPadding));
        touch(updatedAt);
    }

    public static IdCardSetting create(String id, String schoolId, String layout, String profileShape,
            String headerColor, String footerColor, String headerTextColor, String primaryTextColor, int widthMm,
            int heightMm, String bgImageUrl, int bgOpacity, String schoolName, String schoolAddress,
            String principalName, String fields, String staffFields, int validityYears, int logoSize,
            int logoRadius, int signatureSize, int photoSize, int photoPadding, int photoBorder, int cardBorder, int logoBoxSize, int logoPadding) {
        Instant now = Instant.now();
        return new IdCardSetting(id, schoolId, layout, profileShape, headerColor, footerColor, headerTextColor,
                primaryTextColor, widthMm, heightMm, bgImageUrl, bgOpacity, schoolName, schoolAddress, principalName,
                fields, staffFields, validityYears, logoSize, logoRadius, signatureSize, photoSize, photoPadding, photoBorder, cardBorder, logoBoxSize, logoPadding, now, now);
    }

    public void update(String layout, String profileShape, String headerColor, String footerColor,
            String headerTextColor, String primaryTextColor, int widthMm, int heightMm, String bgImageUrl,
            int bgOpacity, String schoolName, String schoolAddress, String principalName, String fields,
            String staffFields, int validityYears, int logoSize,
            int logoRadius, int signatureSize, int photoSize, int photoPadding, int photoBorder, int cardBorder, int logoBoxSize, int logoPadding) {
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
        this.staffFields = staffFields;
        this.validityYears = validityYears > 0 ? validityYears : 1;
        this.logoSize = logoSize > 0 ? logoSize : 100;
        this.logoRadius = Math.max(0, Math.min(50, logoRadius));
        this.signatureSize = signatureSize > 0 ? signatureSize : 100;
        this.photoSize = photoSize > 0 ? photoSize : 100;
        this.photoPadding = Math.max(0, Math.min(24, photoPadding));
        this.photoBorder = Math.max(0, Math.min(12, photoBorder));
        this.cardBorder = Math.max(0, Math.min(12, cardBorder));
        this.logoBoxSize = logoBoxSize > 0 ? logoBoxSize : 100;
        this.logoPadding = Math.max(0, Math.min(24, logoPadding));
        touch(Instant.now());
    }
}
