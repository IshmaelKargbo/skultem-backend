package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewLeaveRequestDTO(

        @NotNull(message = "Approve is required")
        Boolean approve,

        @Size(max = 1000, message = "Note must not exceed 1000 characters")
        String note

) {
}
