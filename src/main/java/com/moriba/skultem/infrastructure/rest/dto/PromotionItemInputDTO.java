package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record PromotionItemInputDTO(
        @NotBlank(message = "Student id is required!") String studentId,
        @NotBlank(message = "Enrollment id is required!") String enrollmentId,
        @NotBlank(message = "Outcome is required!") String outcome,
        String remark,
        String targetStreamId) {
}
