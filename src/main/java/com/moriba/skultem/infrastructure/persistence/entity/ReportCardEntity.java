package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report_cards", indexes = {
        @Index(name = "idx_report_cards_school_id", columnList = "schoolId"),
        @Index(name = "idx_report_cards_class_id", columnList = "classId"),
        @Index(name = "idx_report_cards_term_id", columnList = "termId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCardEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String studentName;

    private String admissionNumber;
    private String photo;

    @Column(nullable = false)
    private String classId;

    private String className;

    @Column(nullable = false)
    private int classSize;

    @Column(nullable = false)
    private String termId;

    private String termName;
    private String academicYearName;

    @Column(nullable = false)
    private double average;

    @Column(nullable = false)
    private int position;

    private String overallGrade;

    @Column(nullable = false)
    private boolean passed;

    private Double attendancePercentage;

    @Column(columnDefinition = "text")
    private String remark;

    @Column(nullable = false, columnDefinition = "text")
    private String subjects;

    private String generatedBy;
    private Instant generatedAt;

    @Column(nullable = false)
    private int downloadCount;

    private Instant createdAt;
    private Instant updatedAt;
}
