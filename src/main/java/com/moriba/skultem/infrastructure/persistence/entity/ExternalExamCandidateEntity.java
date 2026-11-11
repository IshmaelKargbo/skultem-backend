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
@Table(name = "external_exam_candidates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalExamCandidateEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String schoolId;
    @Column(nullable = false)
    private String sittingId;
    @Column(nullable = false)
    private String studentId;
    private String studentName;
    private String admissionNumber;
    @Column(length = 64)
    private String indexNumber;
    @Column(nullable = false, columnDefinition = "text")
    private String scores;
    @Column(nullable = false, columnDefinition = "text")
    @lombok.Builder.Default
    private String grades = "{}";
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
}
