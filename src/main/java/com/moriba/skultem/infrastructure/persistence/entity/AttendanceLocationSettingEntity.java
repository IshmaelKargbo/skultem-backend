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
@Table(name = "attendance_location_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceLocationSettingEntity {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String schoolId;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private int radiusMeters;

    private String allowedIps;

    private Instant createdAt;
    private Instant updatedAt;
}
