package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.LibraryLoan;
import com.moriba.skultem.infrastructure.persistence.entity.LibraryLoanEntity;

public class LibraryLoanMapper {
    public static LibraryLoan toDomain(LibraryLoanEntity param) {
        if (param == null) return null;

        return new LibraryLoan(param.getId(), param.getSchoolId(), LibraryBookMapper.toDomain(param.getBook()),
                param.getBorrowerType(), param.getStudentId(), param.getTeacherId(), param.getBorrowerName(),
                param.getBorrowerClass(), param.getIssueDate(), param.getDueDate(), param.getReturnedAt(),
                param.getStatus(), param.getNotifiedAt(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static LibraryLoanEntity toEntity(LibraryLoan param) {
        if (param == null) return null;

        return LibraryLoanEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .book(LibraryBookMapper.toEntity(param.getBook()))
                .borrowerType(param.getBorrowerType())
                .studentId(param.getStudentId())
                .teacherId(param.getTeacherId())
                .borrowerName(param.getBorrowerName())
                .borrowerClass(param.getBorrowerClass())
                .issueDate(param.getIssueDate())
                .dueDate(param.getDueDate())
                .returnedAt(param.getReturnedAt())
                .status(param.getStatus())
                .notifiedAt(param.getNotifiedAt())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
