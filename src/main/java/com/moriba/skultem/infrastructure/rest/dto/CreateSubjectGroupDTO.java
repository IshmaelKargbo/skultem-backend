package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateSubjectGroupDTO(
                @NotBlank(message = "Name is required") String name,
                String classId,
                String streamId,
                @Min(value = 0, message = "totalSelection must be >= 0") int totalSelection) {
}
