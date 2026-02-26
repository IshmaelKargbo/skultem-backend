package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record SelectedSubjectsDTO(
                @NotNull(message = "Optional subjects are required") List<String> optionalSubjects) {
}
