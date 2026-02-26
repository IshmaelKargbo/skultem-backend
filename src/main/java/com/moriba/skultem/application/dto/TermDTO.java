package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

public record TermDTO(String id, String school, String name, LocalDate startDate, LocalDate endDate, int termNumber,
                AcademicYearDTO academicYear, String status, Instant createdAt,
                Instant updatedAt) {
}
