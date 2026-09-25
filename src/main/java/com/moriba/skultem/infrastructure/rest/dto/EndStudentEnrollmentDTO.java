package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

// exitDate defaults to today; reason is required for an expulsion (checked in the use case).
public record EndStudentEnrollmentDTO(
        @Size(max = 100, message = "Reason is too long") String reason,
        LocalDate exitDate,
        @Size(max = 2000, message = "Note is too long") String note) {
}
