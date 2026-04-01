package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.ExpenseDTO;
import com.moriba.skultem.domain.model.Expense;

public class ExpenseMapper {
    public static ExpenseDTO toDTO(Expense param) {
        return new ExpenseDTO(param.getId(), param.getTitle(), param.getCategory().getName(), param.getAmount(),
                param.getDescription(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
