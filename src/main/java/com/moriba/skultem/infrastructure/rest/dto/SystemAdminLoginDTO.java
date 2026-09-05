package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

// Unlike LoginDTO, no school domain - see SystemAdminLoginUseCase for why a SYSTEM_ADMIN sign-in
// doesn't need one.
public record SystemAdminLoginDTO(

        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        String password) {
}
