package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailVerificationConfirmDTO(@NotBlank(message = "Token is required") @Size(max = 200) String token) {
}
