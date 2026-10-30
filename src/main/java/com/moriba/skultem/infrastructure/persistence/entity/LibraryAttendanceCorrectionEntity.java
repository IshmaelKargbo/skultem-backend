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
@Table(name = "library_attendance_corrections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryAttendanceCorrectionEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String recordId;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String studentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status originalStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status correctedStatus;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false)
    private Instant correctedAt;

    @Column(nullable = false)
    private String correctedBy;

    private String correctedByName;
    private Instant createdAt;
    private Instant updatedAt;
}
