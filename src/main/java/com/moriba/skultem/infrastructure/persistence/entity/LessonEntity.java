package com.moriba.skultem.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state;

    private Instant createdAt;
    private Instant updatedAt;
}
