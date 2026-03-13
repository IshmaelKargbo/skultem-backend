package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignFeeToStudentDTO(
        @NotBlank(message = "Student is required") String studentId,
        @NotBlank(message = "Fee is required") String feeId) {
}
