package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.model.LibraryVisit.Status;

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
@Table(name = "library_visits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryVisitEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String studentName;

    private String studentClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private Instant checkedInAt;

    @Column(nullable = false)
    private String checkedInBy;

    private String checkedInByName;

    @Column(nullable = false)
    private Instant schoolDayEndsAt;

    private Instant checkedOutAt;
    private String checkedOutBy;
    private String checkedOutByName;
    private Instant resolvedAt;
    private String resolvedBy;
    private String resolvedByName;

    @Column(length = 500)
    private String resolutionReason;

    private Instant createdAt;
    private Instant updatedAt;
}
