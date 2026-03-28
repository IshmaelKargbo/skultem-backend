package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Transaction;
import com.moriba.skultem.infrastructure.persistence.entity.TransactionEntity;

public class TransactionMapper {
    public static Transaction toDomain(TransactionEntity param) {
        return new Transaction(param.getId(), param.getSchoolId(), param.getAcademicYearId(), param.getTermId(), param.getTransactionType(), param.getDirection(), param.getAmount(), param.getBalance(), param.getReferenceId(), param.getReferenceType(), param.getCreatedAt());
    }

    public static TransactionEntity toEntity(Transaction param) {
        return TransactionEntity.builder()
                .id(param.getId())
                .academicYearId(param.getAcademicYearId())
                .amount(param.getAmount())
                .direction(param.getDirection())
                .termId(param.getTermId())
                .transactionType(param.getType())
                .balance(param.getBalance())
                .referenceType(param.getReferenceType())
                .referenceId(param.getReferenceId())
                .schoolId(param.getSchoolId())
                .createdAt(param.getCreatedAt())
                .build();
    }
}
