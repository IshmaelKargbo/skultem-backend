package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSchoolDTO(
        @NotBlank(message = "School name is required") @Size(min = 3, max = 150, message = "School name must be between 3 and 150 characters") String name,

        @NotBlank(message = "Domain is required") String domain,

        @NotBlank(message = "Street is required") @Size(min = 5, max = 255, message = "Street must be between 5 and 255 characters") String street,

        @NotBlank(message = "Region is required") String region,

        @NotBlank(message = "District is required") String district,

        @NotBlank(message = "Chiefdom is required") @Size(min = 5, max = 255, message = "Chiefdom must be between 5 and 255 characters") String chiefdom,

        @NotBlank(message = "City is required") String city,

        @NotBlank(message = "Owner email is required") @Email(message = "Invalid email format") String email,

        @NotBlank(message = "Owner phone is required") @Pattern(regexp = "^[0-9+]{7,15}$", message = "Phone number must be between 7 and 15 digits and may include +") String phone,

        @NotBlank(message = "Owner given names are required") @Size(min = 2, max = 100, message = "Given names must be between 2 and 100 characters") String givenNames,

        @NotBlank(message = "Owner family name is required") @Size(min = 2, max = 100, message = "Family name must be between 2 and 100 characters") String familyName,

        @NotBlank(message = "Owner password is required") @Size(min = 8, max = 100, message = "Password must be at least 8 characters") @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).*$", message = "Password must contain at least one uppercase letter, one lowercase letter, and one number") String password
) {
}
