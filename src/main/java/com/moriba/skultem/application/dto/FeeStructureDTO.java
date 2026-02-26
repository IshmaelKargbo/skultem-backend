package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record FeeStructureDTO(String id, ClassDTO clazz, TermDTO term, FeeCategoryDTO category,
        boolean allowInstallment, LocalDate dueDate, AcademicYearDTO academicYear, BigDecimal amount,
        String description, Instant createdAt, Instant updatedAt) {
}
