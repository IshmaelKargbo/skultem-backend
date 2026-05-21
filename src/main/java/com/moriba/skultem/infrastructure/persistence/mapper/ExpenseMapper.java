package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Expense;
import com.moriba.skultem.infrastructure.persistence.entity.ExpenseEntity;

public class ExpenseMapper {
    public static Expense toDomain(ExpenseEntity param) {
        if (param == null) return null;
        var category = ExpenseCategoryMapper.toDomain(param.getCategory());
        return new Expense(param.getId(), param.getSchoolId(), param.getTitle(), param.getAmount(), category,
                param.getDescription(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ExpenseEntity toEntity(Expense param) {
        if (param == null) return null;
        var catrgory = ExpenseCategoryMapper.toEntity(param.getCategory());

        return ExpenseEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .title(param.getTitle())
                .amount(param.getAmount())
                .category(catrgory)
                .description(param.getDescription())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
