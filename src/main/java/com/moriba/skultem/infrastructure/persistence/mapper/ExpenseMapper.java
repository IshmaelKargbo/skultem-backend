package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Expense;
import com.moriba.skultem.infrastructure.persistence.entity.ExpenseEntity;

public class ExpenseMapper {
    public static Expense toDomain(ExpenseEntity param) {
        var category = ExpenseCategoryMapper.toDomain(param.getCategory());
        return new Expense(param.getId(), param.getSchoolId(), param.getTitle(), param.getAmount(), category,
                param.getDescription(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ExpenseEntity toEntity(Expense args) {
        var catrgory = ExpenseCategoryMapper.toEntity(args.getCategory());

        return ExpenseEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .title(args.getTitle())
                .amount(args.getAmount())
                .category(catrgory)
                .description(args.getDescription())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
