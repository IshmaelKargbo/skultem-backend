package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record GradeAssessmentDTO(
                @NotNull(message = "Assessment id is required!") String assessmentId,
                @NotNull(message = "Term id is required!") String termId,
                @NotEmpty(message = "At least one grade must be provided") @Valid List<GradeDTO> grades) {
}
