package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// A class teacher's decision on a class's report cards for a term. ids: only these cards (empty = all of them).
// note: required when sending them back - what needs fixing.
public record ReviewReportCardsDTO(
        @NotBlank(message = "Choose the class") String classId,
        @NotBlank(message = "Choose the term") String termId,
        List<String> ids,
        @Size(max = 500, message = "Keep the note under 500 characters") String note) {
}
