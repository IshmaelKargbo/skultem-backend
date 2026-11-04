package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordDTO(
        @NotBlank(message = "School domain is required") String domain,
        @NotBlank(message = "Email is required") @Size(max = 150, message = "Email must not exceed 150 characters") String email) {
}
