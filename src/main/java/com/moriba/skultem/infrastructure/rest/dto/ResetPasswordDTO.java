package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDTO(
                @NotBlank(message = "Password is required") @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters") String password) {
}
