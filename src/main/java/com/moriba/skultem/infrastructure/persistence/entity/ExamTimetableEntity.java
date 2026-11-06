package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.ExamTimetable;

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
@Table(name = "exam_timetables")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamTimetableEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String termId;

    @Column(nullable = false)
    private String assessmentId;

    @Column(nullable = false, length = 150)
    private String title;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(length = 2000)
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExamTimetable.Status status;

    private Instant publishedAt;
    private String publishedByName;
    private Instant createdAt;
    private Instant updatedAt;
}
