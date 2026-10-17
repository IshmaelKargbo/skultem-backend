package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateContactStatusDTO(
        @NotBlank(message = "Status is required") @Pattern(regexp = "NEW|READ|REPLIED|ARCHIVED", message = "Status must be NEW, READ, REPLIED or ARCHIVED") String status) {
}
