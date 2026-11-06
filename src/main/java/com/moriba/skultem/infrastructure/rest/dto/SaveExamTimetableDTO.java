package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// termId and assessmentId are only read when the timetable is first set up.
public record SaveExamTimetableDTO(
        String termId,
        String assessmentId,
        @NotBlank(message = "Give the timetable a title") @Size(max = 150) String title,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 2000, message = "Keep the instructions under 2000 characters") String instructions) {
}
