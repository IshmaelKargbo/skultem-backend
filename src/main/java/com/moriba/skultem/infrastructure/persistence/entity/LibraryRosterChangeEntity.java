package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.model.LibraryRosterChange.Change;

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
@Table(name = "library_roster_changes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryRosterChangeEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String studentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Change change;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false)
    private Instant changedAt;

    @Column(nullable = false)
    private String changedBy;

    private String changedByName;
    private Instant createdAt;
    private Instant updatedAt;
}
