package com.moriba.skultem.application.dto;

import java.util.List;

public record AssessmentCycleOverviewDTO(
        TermDTO activeTerm,
        int totalClasses,
        int readyClasses,
        int notReadyClasses,
        List<ClassAssessmentCycleStatusDTO> classes) {
}
