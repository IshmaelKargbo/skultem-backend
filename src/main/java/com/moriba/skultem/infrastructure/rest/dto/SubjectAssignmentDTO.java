package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record SubjectAssignmentDTO(
        @NotBlank(message = "Subject is required") String subjectId,
        String groupId, boolean mandatory) {
}
