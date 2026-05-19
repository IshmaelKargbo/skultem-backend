package com.moriba.skultem.application.mapper;

import java.util.List;

import com.moriba.skultem.application.dto.AssessmentDTO;
import com.moriba.skultem.application.dto.AssessmentTemplateDTO;
import com.moriba.skultem.domain.model.AssessmentTemplate;

public class AssessmentTemplateMapper {
    public static AssessmentTemplateDTO toDTO(AssessmentTemplate param, List<AssessmentDTO> assessments) {
        if (param == null) {
            return null;
        }

        return new AssessmentTemplateDTO(
                param.getId(),
                param.getName(),
                param.getDescription(),
                param.getPassMark(),
                assessments,
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static AssessmentTemplateDTO toDTO(AssessmentTemplate param) {
        if (param == null) {
            return null;
        }

        return new AssessmentTemplateDTO(
                param.getId(),
                param.getName(),
                param.getDescription(),
                param.getPassMark(),
                null,
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
