package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.AssessmentDTO;
import com.moriba.skultem.domain.model.Assessment;

public class AssessmentMapper {
    public static AssessmentDTO toDTO(Assessment param) {
        if (param == null) {
            return null;
        }

        return new AssessmentDTO(
                param.getId(),
                param.getName(),
                param.getWeight(),
                param.getPosition(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
