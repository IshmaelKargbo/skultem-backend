package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "library_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibrarySettingEntity {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String schoolId;

    @Column(nullable = false)
    private int maxStudentLoans;

    @Column(nullable = false)
    private int maxTeacherLoans;

    @Column(nullable = false)
    private boolean blockWhenOverdue;

    @Column(nullable = false)
    private int defaultLoanDays;

    private String librarySubjectId;

    private Instant createdAt;
    private Instant updatedAt;
}
