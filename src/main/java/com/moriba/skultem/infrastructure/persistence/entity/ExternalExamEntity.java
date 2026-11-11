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
@Table(name = "external_exams")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalExamEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String schoolId;
    @Column(nullable = false, length = 32)
    private String code;
    @Column(nullable = false)
    private String name;
    private String examBody;
    @Column(length = 32)
    private String level;
    @Column(nullable = false, columnDefinition = "text")
    private String gradeScale;
    @Column(nullable = false, length = 16)
    private String aggregateMode;
    @Column(nullable = false)
    private int bestOf;
    @Column(nullable = false)
    private boolean builtIn;
    @Column(nullable = false, length = 16)
    @lombok.Builder.Default
    private String entryMode = "SCORES";
    private String slipTemplate;
    private String statementTemplate;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
}
