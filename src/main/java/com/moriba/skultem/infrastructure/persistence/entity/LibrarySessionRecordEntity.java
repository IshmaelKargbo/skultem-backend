package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import com.moriba.skultem.domain.model.LibrarySessionRecord.Status;

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
@Table(name = "library_session_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibrarySessionRecordEntity {
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

    private String studentClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(length = 500)
    private String excuseReason;

    @Column(nullable = false)
    private boolean removed;

    @Column(nullable = false)
    private boolean addedManually;

    @Column(nullable = false)
    private boolean corrected;

    private Instant markedAt;
    private String markedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
