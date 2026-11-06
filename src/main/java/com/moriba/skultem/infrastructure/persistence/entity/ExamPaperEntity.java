package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exam_papers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamPaperEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String timetableId;

    @Column(nullable = false)
    private String subjectId;

    private String paper;

    @Column(nullable = false)
    private LocalDate examDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private String venue;
    private String notes;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "exam_paper_classes", joinColumns = @JoinColumn(name = "paper_id"))
    @Column(name = "class_id")
    private Set<String> classIds = new LinkedHashSet<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "exam_paper_invigilators", joinColumns = @JoinColumn(name = "paper_id"))
    @Column(name = "teacher_id")
    private Set<String> invigilatorIds = new LinkedHashSet<>();

    private Instant createdAt;
    private Instant updatedAt;
}
