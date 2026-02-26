package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record AssignSubjectsDTO(
                @NotNull(message = "Assignments are required") List<SubjectAssignmentDTO> assignments) {
}
