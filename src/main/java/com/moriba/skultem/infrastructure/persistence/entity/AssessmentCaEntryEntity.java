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
@Table(name = "assessment_ca_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentCaEntryEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String assessmentScoreId;

    @Column(nullable = false)
    private int entryNumber;

    @Column(nullable = false)
    private int score;

    private String recordedByUserId;

    private Instant createdAt;
    private Instant updatedAt;
}
