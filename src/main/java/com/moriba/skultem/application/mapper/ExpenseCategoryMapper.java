package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ExpenseCategoryDTO;
import com.moriba.skultem.domain.model.ExpenseCategory;

public class ExpenseCategoryMapper {
    public static ExpenseCategoryDTO toDTO(ExpenseCategory param) {
        return new ExpenseCategoryDTO(param.getId(), param.getName(), param.getDescription(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
