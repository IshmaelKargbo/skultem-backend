package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ExpenseCategory;
import com.moriba.skultem.infrastructure.persistence.entity.ExpenseCategoryEntity;

public class ExpenseCategoryMapper {
    public static ExpenseCategory toDomain(ExpenseCategoryEntity param) {
        if (param == null)
            return null;
        return new ExpenseCategory(param.getId(), param.getSchoolId(), param.getName(), param.getDscription(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ExpenseCategoryEntity toEntity(ExpenseCategory param) {
        if (param == null)
            return null;
        return ExpenseCategoryEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .name(param.getName())
                .dscription(param.getDescription())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
