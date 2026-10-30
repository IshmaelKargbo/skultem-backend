package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.moriba.skultem.domain.model.LibrarySession.Status;

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
@Table(name = "library_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibrarySessionEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    private String timetableEntryId;

    @Column(name = "session_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String classSessionId;

    @Column(nullable = false)
    private String className;

    private String subjectId;

    @Column(nullable = false)
    private String subjectName;

    private String periodName;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private String roomName;
    private String termId;

    @Column(nullable = false)
    private LocalDate termEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    private Instant openedAt;
    private String openedBy;
    private String openedByName;
    private Instant finalizedAt;
    private String finalizedBy;
    private String finalizedByName;
    private Instant cancelledAt;
    private String cancelledBy;
    private String cancelledByName;

    @Column(length = 500)
    private String cancelReason;

    private Instant createdAt;
    private Instant updatedAt;
}
