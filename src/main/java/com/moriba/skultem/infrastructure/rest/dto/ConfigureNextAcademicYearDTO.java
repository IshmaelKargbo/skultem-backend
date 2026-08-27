package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;

/**
 * All fields are optional overrides - anything left null is derived from the source academic
 * year (name incremented, dates shifted forward by one year).
 */
public record ConfigureNextAcademicYearDTO(String name, LocalDate startDate, LocalDate endDate) {
}
