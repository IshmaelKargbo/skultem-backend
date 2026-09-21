package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeEnrollmentClassDTO(
                @NotBlank(message = "Class is required") String classId,
                @NotBlank(message = "Section is required") String sectionId,
                String streamId) {
}
