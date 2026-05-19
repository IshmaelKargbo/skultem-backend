package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.StudentParent;
import com.moriba.skultem.infrastructure.persistence.entity.ParentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StudentParentEntity;

public class StudentParentMapper {
    public static StudentParent toDomain(StudentParentEntity param) {
        if (param == null) {
            return null;
        }

        Parent parent = ParentMapper.toDomain(param.getParent());
        Student student = StudentMapper.toDomain(param.getStudent());

        return new StudentParent(param.getId(), param.getSchoolId(), param.getRelationship(), parent, student, param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static StudentParentEntity toEntity(StudentParent param) {
        if (param == null) {
            return null;
        }

        StudentEntity student = StudentMapper.toEntity(param.getStudent());
        ParentEntity parent = ParentMapper.toEntity(param.getParent());

        return StudentParentEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .status(param.getStatus())
                .student(student)
                .parent(parent)
                .relationship(param.getRelationship())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
