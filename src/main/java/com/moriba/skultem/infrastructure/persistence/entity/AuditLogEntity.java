package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.model.AuditLog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity {
    @Id
    private String id;

    private String schoolId;

    @Column(nullable = false)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = true)
    private AcademicYearEntity academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private UserEntity user;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private String device;

    @Column(nullable = false)
    private String deviceType;

    @Column(nullable = false)
    private String os;

    @Column(nullable = false)
    private String browser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditLog.Status status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    private Instant createdAt;
    private Instant updatedAt;
}
