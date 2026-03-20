package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.infrastructure.persistence.entity.ClassSessionEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ParentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;

public class StudentMapper {
    public static Student toDomain(StudentEntity param) {
        Parent parent = ParentMapper.toDomain(param.getParent());
        ClassSession session = ClassSessionMapper.toDomain(param.getSession());

        return new Student(param.getId(), param.getSchoolId(), param.getAdmissionNumber(), param.getGivenNames(),
                param.getFamilyName(), session, param.getGender(), parent, param.getDateOfBirth(), param.getStatus(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static StudentEntity toEntity(Student param) {
        ParentEntity parent = ParentMapper.toEntity(param.getParent());
        ClassSessionEntity session = ClassSessionMapper.toEntity(param.getSession());

        return StudentEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .admissionNumber(param.getAdmissionNumber())
                .givenNames(param.getGivenNames())
                .familyName(param.getFamilyName())
                .dateOfBirth(param.getDateOfBirth())
                .gender(param.getGender())
                .session(session)
                .parent(parent)
                .status(param.getStatus())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
