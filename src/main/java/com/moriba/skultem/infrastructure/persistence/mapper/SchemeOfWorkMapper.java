package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.SchemeOfWork;
import com.moriba.skultem.infrastructure.persistence.entity.SchemeOfWorkEntity;

public class SchemeOfWorkMapper {
    public static SchemeOfWork toDomain(SchemeOfWorkEntity param) {
        var term = TermMapper.toDomain(param.getTerm());
        var subject = SubjectMapper.toDomain(param.getSubject());
        var session = ClassSessionMapper.toDomain(param.getSession());

        return new SchemeOfWork(param.getId(), param.getSchoolId(), subject, session, term, param.getWeeks(),
                param.getState(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static SchemeOfWorkEntity toEntity(SchemeOfWork param) {
        return SchemeOfWorkEntity.builder()
                .id(param.getId())
                .term(TermMapper.toEntity(param.getTerm()))
                .subject(SubjectMapper.toEntity(param.getSubject()))
                .session(ClassSessionMapper.toEntity(param.getSession()))
                .weeks(param.getWeeks())
                .state(param.getState())
                .schoolId(param.getSchoolId())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
