package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "management_sections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagementSectionEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int displayOrder;

    private String logo;

    @Column(name = "principal_name")
    private String principalName;

    @Column(name = "principal_signature")
    private String principalSignature;

    private String phone;

    @Column(name = "attendance_threshold")
    private Double attendanceThreshold;

    @Column(name = "attendance_window_days")
    private Integer attendanceWindowDays;

    @Column(name = "attendance_min_days")
    private Integer attendanceMinDays;

    @Column(name = "attendance_streak_days")
    private Integer attendanceStreakDays;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "grade_approver")
    private com.moriba.skultem.domain.vo.GradeApprover gradeApprover;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String address;

    private Instant createdAt;

    private Instant updatedAt;
}
