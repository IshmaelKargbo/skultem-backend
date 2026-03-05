package com.moriba.skultem.application.dto;

import java.util.List;

public record ActiveAssessmentCycleDTO(
        TermDTO activeTerm,
        String templateId,
        String templateName,
        String templateDescription,
        List<AssessmentCycleDTO> assessments,
        int totalWeight,
        boolean ready) {
}
