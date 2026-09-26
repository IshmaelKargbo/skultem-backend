package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;

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
@Table(name = "assessment_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentConfigurationEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    private String managementSectionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentStructure structure;

    @Column(nullable = false)
    private int caPercentage;

    @Column(nullable = false)
    private int formalPercentage;

    @Enumerated(EnumType.STRING)
    private CaFrequency caFrequency;

    @Column(nullable = false)
    private int caEntries;

    @Column(nullable = false)
    private int version;

    private String updatedByUserId;

    @Column(columnDefinition = "TEXT")
    private String planText;

    private Instant createdAt;
    private Instant updatedAt;
}
