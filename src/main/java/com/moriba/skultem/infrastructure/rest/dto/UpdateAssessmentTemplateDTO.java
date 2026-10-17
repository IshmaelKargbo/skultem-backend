package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateAssessmentTemplateDTO(
        String name,
        Integer passMark,
        String description,
        @NotNull(message = "Assessments are required") @NotEmpty(message = "At least one assessment is required") List<Item> assessments,
        // Change it even though tests were already submitted, or drop a test that has marks.
        Boolean force) {

    public record Item(String id, String name, Integer weight) {
    }
}
