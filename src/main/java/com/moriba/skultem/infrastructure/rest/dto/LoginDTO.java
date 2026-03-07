package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

public record LoginDTO(
        @NotNull(message = "School domain is required") String domain,

        @NotNull(message = "Email is required") String email,

        @NotNull(message = "Password are required") String password) {
}
