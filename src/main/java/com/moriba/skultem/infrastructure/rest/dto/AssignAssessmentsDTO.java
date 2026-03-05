package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record AssignAssessmentsDTO(
        @NotNull(message = "Assessment assignments are required") List<AssessmentAssignmentDTO> assignments) {
}
