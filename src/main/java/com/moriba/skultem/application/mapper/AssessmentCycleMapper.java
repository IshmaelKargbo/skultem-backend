package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AssessmentCycleDTO;
import com.moriba.skultem.domain.model.Assessment;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;

public class AssessmentCycleMapper {
    public static AssessmentCycleDTO toDTO(ClassSubjectAssessmentLifeCycle param) {
        return toDTO(param, null);
    }

    /** {@code returnReason}: the approver's note if this cycle was sent back - see AssessmentCycleDTO. */
    public static AssessmentCycleDTO toDTO(ClassSubjectAssessmentLifeCycle param, String returnReason) {
        if (param == null) {
            return null;
        }

        var assessment = param.getAssessment();

        return new AssessmentCycleDTO(assessment.getId(), assessment.getName(), assessment.getWeight(),
                assessment.getPosition(), param.getStatus().name(), returnReason);
    }

    public static AssessmentCycleDTO toDTO(Assessment assessment, String status) {
        if (assessment == null) {
            return null;
        }

        return new AssessmentCycleDTO(assessment.getId(), assessment.getName(), assessment.getWeight(),
                assessment.getPosition(), status, null);
    }
}
