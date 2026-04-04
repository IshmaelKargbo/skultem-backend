package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.TransactionDTO;
import com.moriba.skultem.domain.model.Transaction;

public class TransactionMapper {

    public static TransactionDTO toDTO(Transaction param) {
        if (param == null)
            return null;

        return new TransactionDTO(param.getId(), param.getAcademicYear().getName(), param.getTerm().getName(), param.getType(),
                param.getDirection(), param.getAmount(), param.getBalance(), param.getReferenceId(),
                param.getReferenceType(), param.getCreatedAt());
    }
}