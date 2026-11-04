package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetNewPasswordDTO(
        @NotBlank(message = "Reset link is invalid") @Size(max = 200, message = "Reset link is invalid") String token,
        @NotBlank(message = "Password is required") @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters") String password) {
}
