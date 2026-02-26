package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AcademicYearDTO(String id, String school, String name, LocalDate startDate, LocalDate endDate, Boolean active, String status, Instant createdAt,
        Instant updatedAt) {
}
