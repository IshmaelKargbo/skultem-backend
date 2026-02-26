package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ClassMaster;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.infrastructure.persistence.entity.ClassMasterEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassSessionEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TeacherEntity;

public class ClassMasterMapper {
    public static ClassMaster toDomain(ClassMasterEntity param) {
        ClassSession session = null;
        Teacher teacher = null;

        if (param.getSession() != null) {
            session = ClassSessionMapper.toDomain(param.getSession());
        }

        if (param.getTeacher() != null) {
            teacher = TeacherMapper.toDomain(param.getTeacher());
        }

        return new ClassMaster(param.getId(), param.getSchoolId(), session, teacher,
                param.getAssignedAt(), param.getEndedAt(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ClassMasterEntity toEntity(ClassMaster args) {
        TeacherEntity teacher = null;
        ClassSessionEntity session = null;

        if (args.getSession() != null) {
            session = ClassSessionMapper.toEntity(args.getSession());
        }

        if (args.getTeacher() != null) {
            teacher = TeacherMapper.toEntity(args.getTeacher());
        }

        return ClassMasterEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .assignedAt(args.getAssignAt())
                .teacher(teacher)
                .session(session)
                .endedAt(args.getEndedAt())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
