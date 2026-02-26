package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateEnrollmentDTO(
                @NotBlank(message = "Class session is required") String classId,
                @NotBlank(message = "Section is required") String sectionId,
                @NotNull(message = "Students are required") @NotEmpty(message = "Students cannot be empty") List<String> students,
                String streamId) {
}
