package com.moriba.skultem.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "national_academic_years")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NationalAcademicYearEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    private Instant createdAt;
    private Instant updatedAt;
}
