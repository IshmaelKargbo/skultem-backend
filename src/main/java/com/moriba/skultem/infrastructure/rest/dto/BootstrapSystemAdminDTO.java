package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// See BootstrapSystemAdminUseCase - the only path onto Role.SYSTEM_ADMIN. domain identifies the
// existing school this account's SchoolUser row (and therefore its login) is anchored to, same as
// every other role - SYSTEM_ADMIN just isn't scoped to it once granted (see PermissionService).
public record BootstrapSystemAdminDTO(

        @NotBlank(message = "Bootstrap token is required")
        String token,

        @NotBlank(message = "School domain is required")
        String domain,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Given names are required")
        String givenNames,

        @NotBlank(message = "Family name is required")
        String familyName) {
}
