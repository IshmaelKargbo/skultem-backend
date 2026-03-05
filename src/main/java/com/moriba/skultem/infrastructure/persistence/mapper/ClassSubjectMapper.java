package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ClassSubject;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.infrastructure.persistence.entity.ClassEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassSubjectEntity;
import com.moriba.skultem.infrastructure.persistence.entity.SubjectEntity;
import com.moriba.skultem.infrastructure.persistence.entity.SubjectGroupEntity;

public class ClassSubjectMapper {
    public static ClassSubject toDomain(ClassSubjectEntity param) {
        Subject subject = null;
        Clazz clazz = null;
        SubjectGroup group = null;

        if (param.getClazz() != null) {
            clazz = ClassMapper.toDomain(param.getClazz());
        }

        if (param.getSubject() != null) {
            subject = SubjectMapper.toDomain(param.getSubject());
        }

        if (param.getGroup() != null) {
            group = SubjectGroupMapper.toDomain(param.getGroup());
        }

        return new ClassSubject(param.getId(), param.getSchoolId(), clazz, subject,
                group, param.getMandatory(), param.getLocked(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ClassSubjectEntity toEntity(ClassSubject param) {
        ClassEntity clazz = null;
        SubjectEntity subject = null;
        SubjectGroupEntity group = null;

        if (param.getClazz() != null) {
            clazz = ClassMapper.toEntity(param.getClazz());
        }

        if (param.getSubject() != null) {
            subject = SubjectMapper.toEntity(param.getSubject());
        }

        if (param.getGroup() != null) {
            group = SubjectGroupMapper.toEntity(param.getGroup());
        }

        return ClassSubjectEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .clazz(clazz)
                .subject(subject)
                .group(group)
                .locked(param.getLocked())
                .mandatory(param.getMandatory())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}

