package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.moriba.skultem.domain.model.School.GenderComposition;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.model.School.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "schools")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String domain;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String address;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String owner;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String gradingScale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private String logo;

    private String motto;

    @Column(name = "principal_name")
    private String principalName;

    @Column(name = "principal_signature")
    private String principalSignature;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "secondary_color")
    private String secondaryColor;

    @Column(name = "attendance_threshold")
    private Double attendanceThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_composition", nullable = false)
    private GenderComposition genderComposition;

    @Column(name = "is_test_school", nullable = false)
    private boolean testSchool;

    @Enumerated(EnumType.STRING)
    @Column(name = "management_model", nullable = false)
    private ManagementModel managementModel;

    private Instant createdAt;
    private Instant updatedAt;
}
