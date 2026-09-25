package com.moriba.skultem.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginDTO(
        @NotNull(message = "School domain is required") String domain,

        // Email or phone number - see LoginUseCase. Still accepted as "email" so existing clients
        // keep working.
        @JsonAlias({ "email", "phone" })
        @NotBlank(message = "Email or phone number is required") String identifier,

        @NotNull(message = "Password are required") String password) {
}
