package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;

public class StudentMapper {
    public static Student toDomain(StudentEntity param) {
        return new Student(param.getId(), param.getSchoolId(), param.getAdmissionNumber(), param.getGivenNames(), param.getFamilyName(), param.getGender(), param.getDateOfBirth(), param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static StudentEntity toEntity(Student args) {
        return StudentEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .admissionNumber(args.getAdmissionNumber())
                .givenNames(args.getGivenNames())
                .familyName(args.getFamilyName())
                .dateOfBirth(args.getDateOfBirth())
                .gender(args.getGender())
                .status(args.getStatus())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
