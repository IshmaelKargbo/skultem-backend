package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.infrastructure.persistence.entity.StudentLedgerEntryEntity;

public class StudentLedgerEntryMapper {
    public static StudentLedgerEntry toDomain(StudentLedgerEntryEntity param) {
        return new StudentLedgerEntry(param.getId(), param.getSchoolId(), param.getAcademicYearId(),
                param.getStudentId(), param.getTermId(), param.getTransactionType(), param.getDirection(),
                param.getAmount(), param.getReferenceId(), param.getDescription(), param.getPaidAt(),
                param.getBalance(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static StudentLedgerEntryEntity toEntity(StudentLedgerEntry param) {
        return StudentLedgerEntryEntity.builder()
                .id(param.getId())
                .academicYearId(param.getAcademicYearId())
                .amount(param.getAmount())
                .description(param.getDescription())
                .direction(param.getDirection())
                .termId(param.getTermId())
                .transactionType(param.getTransactionType())
                .studentId(param.getStudentId())
                .balance(param.getBalance())
                .paidAt(param.getPaidAt())
                .referenceId(param.getReferenceId())
                .schoolId(param.getSchoolId())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
