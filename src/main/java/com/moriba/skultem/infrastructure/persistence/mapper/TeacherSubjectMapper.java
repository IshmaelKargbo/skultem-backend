package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.infrastructure.persistence.entity.ClassSessionEntity;
import com.moriba.skultem.infrastructure.persistence.entity.SubjectEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TeacherEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TeacherSubjectEntity;

public class TeacherSubjectMapper {
    public static TeacherSubject toDomain(TeacherSubjectEntity param) {
        Teacher teacher = null;
        Subject subject = null;
        ClassSession session = null;

        if (param.getTeacher() != null) {
            teacher = TeacherMapper.toDomain(param.getTeacher());
        }

        if (param.getSubject() != null) {
            subject = SubjectMapper.toDomain(param.getSubject());
        }

        if (param.getSession() != null) {
            session = ClassSessionMapper.toDomain(param.getSession());
        }

        return new TeacherSubject(param.getId(), param.getSchoolId(), session, teacher, subject, param.getAssignedAt(),
                param.getCreatedAt(), param.getUpdatedAt());
    }

    public static TeacherSubjectEntity toEntity(TeacherSubject param) {
        TeacherEntity teacher = null;
        SubjectEntity subject = null;
        ClassSessionEntity session = null;

        if (param.getTeacher() != null) {
            teacher = TeacherMapper.toEntity(param.getTeacher());
        }

        if (param.getSubject() != null) {
            subject = SubjectMapper.toEntity(param.getSubject());
        }

        if (param.getSession() != null) {
            session = ClassSessionMapper.toEntity(param.getSession());
        }

        return TeacherSubjectEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .session(session)
                .subject(subject)
                .teacher(teacher)
                .assignedAt(param.getAssignedAt())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
