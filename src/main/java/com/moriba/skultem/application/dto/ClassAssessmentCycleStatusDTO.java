package com.moriba.skultem.application.dto;

public record ClassAssessmentCycleStatusDTO(
        String classId,
        String className,
        String templateId,
        String templateName,
        int assessmentCount,
        int totalWeight,
        boolean templateLocked,
        boolean ready,
        String note) {
}
