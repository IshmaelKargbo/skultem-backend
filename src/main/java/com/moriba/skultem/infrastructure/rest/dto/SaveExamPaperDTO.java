package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveExamPaperDTO(
        @NotBlank(message = "Choose a subject") String subjectId,
        @Size(max = 100) String paper,
        @NotNull(message = "Choose the exam date") LocalDate examDate,
        @NotNull(message = "Choose a start time") LocalTime startTime,
        @NotNull(message = "Choose an end time") LocalTime endTime,
        @NotEmpty(message = "Choose at least one class") List<String> classIds,
        List<String> invigilatorIds,
        @Size(max = 150) String venue,
        @Size(max = 500) String notes) {
}
