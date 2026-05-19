package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleDTO(
        @NotBlank(message = "User is required") String userId,

        @NotBlank(message = "Role is required") String role) {
}
