package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserDTO(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

        @NotBlank(message = "Role is required") String role,

        @NotBlank(message = "Given names are required") @Size(min = 2, max = 100, message = "Given names must be between 2 and 100 characters") String givenNames,

        @NotBlank(message = "Family name is required") @Size(min = 2, max = 100, message = "Family name must be between 2 and 100 characters") String familyName,

        @NotBlank(message = "Password is required") @Size(min = 8, max = 100, message = "Password must be at least 8 characters") @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).*$", message = "Password must contain at least one uppercase letter, one lowercase letter, and one number") String password) {
}
