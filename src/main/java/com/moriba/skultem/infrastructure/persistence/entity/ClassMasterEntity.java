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
@Table(name = "class_masters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassMasterEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSessionEntity session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private TeacherEntity teacher;

    @Column(nullable = false, name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    private Instant createdAt;

    private Instant updatedAt;
}
