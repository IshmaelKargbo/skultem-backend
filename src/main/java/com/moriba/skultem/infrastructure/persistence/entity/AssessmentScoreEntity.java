package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
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
@Table(name = "assessment_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentScoreEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_assessment_id", nullable = false)
    private StudentAssessmentEntity studentAssessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_subject_assessment_life_cycle_id", nullable = false)
    private ClassSubjectAssessmentLifeCycleEntity cycle;

    @Column(nullable = false)
    private Integer weight;

    @Column(nullable = false)
    private Integer score;

    private Instant createdAt;

    private Instant updatedAt;
}
