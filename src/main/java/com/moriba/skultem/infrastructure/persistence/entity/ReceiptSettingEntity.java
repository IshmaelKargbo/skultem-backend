package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "receipt_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptSettingEntity {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String schoolId;

    @Column(nullable = false)
    private String accentColor;

    private String logoUrl;
    private String footerNote;

    @Column(nullable = false)
    private boolean showWatermark;

    @Column(nullable = false)
    private boolean showAmountInWords;

    private Instant createdAt;
    private Instant updatedAt;
}
