package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.model.StudentFollowUp;

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
@Table(name = "student_follow_ups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFollowUpEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String schoolId;
    @Column(nullable = false)
    private String studentId;
    @Column(nullable = false)
    private String classId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StudentFollowUp.Action action;
    @Column(columnDefinition = "text")
    private String note;
    @Column(nullable = false)
    private boolean resolved;
    private String recordedByUserId;
    private String recordedByName;
    @Column(nullable = false)
    private Instant createdAt;
}
