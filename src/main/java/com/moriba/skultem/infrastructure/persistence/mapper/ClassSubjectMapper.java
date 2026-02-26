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
                group, param.getMandatory(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ClassSubjectEntity toEntity(ClassSubject args) {
        ClassEntity clazz = null;
        SubjectEntity subject = null;
        SubjectGroupEntity group = null;

        if (args.getClazz() != null) {
            clazz = ClassMapper.toEntity(args.getClazz());
        }

        if (args.getSubject() != null) {
            subject = SubjectMapper.toEntity(args.getSubject());
        }

        if (args.getGroup() != null) {
            group = SubjectGroupMapper.toEntity(args.getGroup());
        }

        return ClassSubjectEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .clazz(clazz)
                .subject(subject)
                .group(group)
                .mandatory(args.getMandatory())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
