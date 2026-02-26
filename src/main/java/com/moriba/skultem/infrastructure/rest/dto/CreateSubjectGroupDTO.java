package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSubjectGroupDTO(
                @NotBlank(message = "Name is required") String name,
                @NotBlank(message = "Level is required") String level,
                String classId,
                String streamId,
                @NotNull(message = "Required is required") Boolean required,
                @Min(value = 0, message = "minSelection must be >= 0") int minSelection,
                @Min(value = 0, message = "maxSelection must be >= 0") int maxSelection,
                @Min(value = 0, message = "displayOrder must be >= 0") int displayOrder) {
}
