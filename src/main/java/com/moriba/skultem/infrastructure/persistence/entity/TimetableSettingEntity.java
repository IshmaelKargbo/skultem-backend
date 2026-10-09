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
@Table(name = "timetable_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableSettingEntity {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String schoolId;

    @Column(nullable = false)
    private String title;

    private String accentColor;

    @Column(nullable = false)
    private String orientation;

    @Column(nullable = false)
    private boolean showLogo;

    @Column(nullable = false)
    private boolean showIcons;

    @Column(nullable = false)
    private boolean showTeacher;

    @Column(nullable = false)
    private boolean showRoom;

    @Column(nullable = false)
    private boolean showPeriodTimes;

    private String footerNote;

    private Instant createdAt;
    private Instant updatedAt;
}
