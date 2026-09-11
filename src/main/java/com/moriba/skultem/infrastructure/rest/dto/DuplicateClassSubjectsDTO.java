package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record DuplicateClassSubjectsDTO(
        @NotEmpty(message = "Select at least one class to duplicate the subjects into") List<String> targetClassIds) {
}
