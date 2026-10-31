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
@Table(name = "health_care_turned_away")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthCareTurnedAwayEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String studentName;

    private String studentClass;

    @Column(nullable = false)
    private Instant turnedAwayAt;

    @Column(nullable = false, length = 1000)
    private String complaints;

    @Column(nullable = false)
    private String ruleName;

    private int maxVisits;

    @Column(nullable = false, length = 20)
    private String period;

    private int visitCount;

    @Column(nullable = false)
    private String recordedBy;

    private String recordedByName;
    private Instant createdAt;
}
