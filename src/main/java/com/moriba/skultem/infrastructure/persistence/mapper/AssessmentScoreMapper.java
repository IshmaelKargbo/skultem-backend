package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.StudentAssessment;
import com.moriba.skultem.infrastructure.persistence.entity.AssessmentScoreEntity;
import com.moriba.skultem.infrastructure.persistence.entity.ClassSubjectAssessmentLifeCycleEntity;
import com.moriba.skultem.infrastructure.persistence.entity.StudentAssessmentEntity;

public class AssessmentScoreMapper {
    public static AssessmentScore toDomain(AssessmentScoreEntity param) {
        if (param == null) {
            return null;
        }

        StudentAssessment studentAssessment = StudentAssessmentMapper.toDomain(param.getStudentAssessment());
        ClassSubjectAssessmentLifeCycle cycle = ClassSubjectAssessmentLifeCycleMapper.toDomain(param.getCycle()); 
        
        return new AssessmentScore(param.getId(), param.getSchoolId(), studentAssessment, cycle, param.getWeight(),
                param.getScore(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static AssessmentScoreEntity toEntity(AssessmentScore param) {
        if (param == null) {
            return null;
        }

        StudentAssessmentEntity studentAssessment = StudentAssessmentMapper.toEntity(param.getStudentAssessment());
        ClassSubjectAssessmentLifeCycleEntity cycle = ClassSubjectAssessmentLifeCycleMapper.toEntity(param.getCycle());

        return AssessmentScoreEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .studentAssessment(studentAssessment)
                .weight(param.getWeight())
                .score(param.getScore())
                .cycle(cycle)
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
