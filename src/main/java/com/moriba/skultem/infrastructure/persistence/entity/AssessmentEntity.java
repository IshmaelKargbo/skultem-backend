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
@Table(name = "assessments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private AssessmentTemplateEntity template;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer weight;

    @Column(nullable = false)
    private int position;

    private Instant createdAt;

    private Instant updatedAt;
}
