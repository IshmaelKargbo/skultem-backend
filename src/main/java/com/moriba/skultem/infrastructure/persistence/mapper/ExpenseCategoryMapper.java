package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ExpenseCategory;
import com.moriba.skultem.infrastructure.persistence.entity.ExpenseCategoryEntity;

public class ExpenseCategoryMapper {
    public static ExpenseCategory toDomain(ExpenseCategoryEntity param) {
        return new ExpenseCategory(param.getId(), param.getSchoolId(), param.getName(), param.getDscription(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ExpenseCategoryEntity toEntity(ExpenseCategory args) {
        return ExpenseCategoryEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .name(args.getName())
                .dscription(args.getDescription())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
