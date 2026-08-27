package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

public record AssignNextAcademicYearDTO(
        @NotNull(message = "Next academic year is required") String nextYearId) {
}
