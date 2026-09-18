package com.moriba.skultem.application.dto;

import java.util.List;

// Result of GetAssessmentCompletionReportUseCase - page/size/total mirror how
// GetClassAcademicPerformanceUseCase paginates its student list, since a whole-school completion
// report (one row per class x subject x assessment) can also run into the hundreds of rows.
public record AssessmentCompletionReportDTO(
        List<AssessmentCompletionRowDTO> rows,
        int page,
        int size,
        int total) {
}
