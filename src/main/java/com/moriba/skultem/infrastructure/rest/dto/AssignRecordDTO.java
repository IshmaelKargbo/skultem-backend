package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRecordDTO(

        @NotBlank(message = "Student is required")
        String id,

        @NotBlank(message = "House is required")
        String house

) {
}