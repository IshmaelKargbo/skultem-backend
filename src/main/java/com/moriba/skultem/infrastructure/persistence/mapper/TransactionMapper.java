package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Transaction;
import com.moriba.skultem.infrastructure.persistence.entity.TransactionEntity;

public class TransactionMapper {
    public static Transaction toDomain(TransactionEntity param) {
        var term = TermMapper.toDomain(param.getTerm());
        var academicYear = AcademicYearMapper.toDomain(param.getAcademicYear());
        return new Transaction(param.getId(), param.getSchoolId(), academicYear, term, param.getTransactionType(),
                param.getDirection(), param.getAmount(), param.getBalance(), param.getReferenceId(),
                param.getReferenceType(), param.getCreatedAt());
    }

    public static TransactionEntity toEntity(Transaction param) {
        var academicYear = AcademicYearMapper.toEntity(param.getAcademicYear());
        var term  = TermMapper.toEntity(param.getTerm());

        return TransactionEntity.builder()
                .id(param.getId())
                .academicYear(academicYear)
                .amount(param.getAmount())
                .direction(param.getDirection())
                .term(term)
                .transactionType(param.getType())
                .balance(param.getBalance())
                .referenceType(param.getReferenceType())
                .referenceId(param.getReferenceId())
                .schoolId(param.getSchoolId())
                .createdAt(param.getCreatedAt())
                .build();
    }
}
