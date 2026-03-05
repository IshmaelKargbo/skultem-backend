package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.infrastructure.persistence.entity.ClassSubjectAssessmentLifeCycleEntity;

public class ClassSubjectAssessmentLifeCycleMapper {
    public static ClassSubjectAssessmentLifeCycle toDomain(ClassSubjectAssessmentLifeCycleEntity param) {
        if (param == null) {
            return null;
        }

        var subject = TeacherSubjectMapper.toDomain(param.getSubject());
        var term = TermMapper.toDomain(param.getTerm());
        var assessment = AssessmentMapper.toDomain(param.getAssessment());

        return new ClassSubjectAssessmentLifeCycle(param.getId(), param.getSchoolId(), subject, term, assessment, param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static ClassSubjectAssessmentLifeCycleEntity toEntity(ClassSubjectAssessmentLifeCycle param) {
        if (param == null) {
            return null;
        }

        var term = TermMapper.toEntity(param.getTerm());
        var assessment = AssessmentMapper.toEntity(param.getAssessment());
        var subject = TeacherSubjectMapper.toEntity(param.getSubject());

        return new ClassSubjectAssessmentLifeCycleEntity(param.getId(), param.getSchoolId(), term, assessment, subject, param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
