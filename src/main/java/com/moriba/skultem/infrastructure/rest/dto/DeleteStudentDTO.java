package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteStudentDTO(@NotBlank(message = "Type the admission number to confirm") String confirmation) {
}
