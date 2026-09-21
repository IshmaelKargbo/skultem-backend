package com.moriba.skultem.infrastructure.persistence.entity;

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
@Table(name = "national_terms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NationalTermEntity {
    @Id
    private String id;

    @Column(name = "national_academic_year_id", nullable = false)
    private String nationalAcademicYearId;

    @Column(nullable = false)
    private int termNumber;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;
}
