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
@Table(name = "report_card_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCardSettingEntity {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String schoolId;

    @Column(nullable = false)
    private String headerColor;

    private String logoUrl;
    private String footerNote;

    @Column(nullable = false)
    private boolean showAttendance;

    @Column(nullable = false)
    private boolean showRemarks;

    @Column(nullable = false)
    private boolean showPosition;

    @Column(nullable = false)
    private boolean showSignatures;

    @Column(nullable = false)
    private boolean showGradeScale;

    private Instant createdAt;
    private Instant updatedAt;
}
