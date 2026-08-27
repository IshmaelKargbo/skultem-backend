package com.moriba.skultem.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.moriba.skultem.domain.model.Lesson.State;

@Entity
@Table(name = "lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_id", nullable = false)
    private WeekEntity week;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String lesson;

    @Column(nullable = false)
    private LocalDate date;

    private String duration;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String objectives;

    @Column(columnDefinition = "text")
    private String previousKnowledge;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String teachingAids;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String referenceMaterials;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String presentation;

    @Column(columnDefinition = "text")
    private String evaluation;

    @Column(columnDefinition = "text")
    private String assignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state;

    private Instant createdAt;
    private Instant updatedAt;
}
