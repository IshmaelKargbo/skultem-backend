package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.Attendance;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private EnrollmentEntity enrollment;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private boolean present;

    @Column(nullable = false)
    private boolean excused;

    @Column(nullable = false)
    private boolean late;

    private String reason;

    @Column(nullable = false)
    private boolean holiday;

    private String recordedByUserId;

    // Each roll call's mark on a two-roll-call day; null otherwise (see Attendance.markRoll).
    @Enumerated(EnumType.STRING)
    @Column(name = "first_roll", length = 16)
    private Attendance.RollMark firstRoll;

    @Enumerated(EnumType.STRING)
    @Column(name = "second_roll", length = 16)
    private Attendance.RollMark secondRoll;

    private Instant createdAt;
    private Instant updatedAt;
}
