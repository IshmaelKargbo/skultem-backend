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
@Table(name = "external_exam_sittings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalExamSittingEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String schoolId;
    @Column(nullable = false)
    private String examId;
    @Column(nullable = false)
    private String classId;
    @Column(nullable = false)
    private String academicYearId;
    @Column(length = 64)
    private String centreNumber;
    @Column(nullable = false, columnDefinition = "text")
    private String subjects;
    @Column(nullable = false, length = 16)
    private String status;
    private String confirmedByName;
    private Instant confirmedAt;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
}
