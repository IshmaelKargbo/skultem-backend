package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.moriba.skultem.domain.model.FeeStructure.Type;
import com.moriba.skultem.domain.vo.Gender;

public record FeeStructureDTO(String id, Type type, ClassDTO clazz, TermDTO term, FeeCategoryDTO category,
        boolean allowInstallment, boolean hasSupply, List<FeeStructureSupplyItemDTO> supplyItems, LocalDate dueDate,
        AcademicYearDTO academicYear, BigDecimal amount,
        String description, boolean isSystem, boolean newStudentsOnly, boolean oldStudentsOnly, Gender gender,
        Instant createdAt, Instant updatedAt) {
}
