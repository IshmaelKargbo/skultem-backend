package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.FeeStructure.Type;

public record FeeStructureDTO(String id, Type type, ClassDTO clazz, TermDTO term, FeeCategoryDTO category,
        boolean allowInstallment, boolean hasSupply, int totalSupply, MaterialDTO material, LocalDate dueDate, AcademicYearDTO academicYear, BigDecimal amount,
        String description, Instant createdAt, Instant updatedAt) {
}
