package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateParentDTO(
                @NotBlank(message = "Given names are required") @Size(min = 2, max = 100, message = "Given names must be between 2 and 100 characters") String givenNames,

                @NotBlank(message = "Family name is required") @Size(min = 2, max = 100, message = "Family name must be between 2 and 100 characters") String familyName,

                @NotBlank(message = "Email is required") @Email(message = "Invalid email format") @Size(max = 150, message = "Email must not exceed 150 characters") String email,

                @NotBlank(message = "Phone is required") @Pattern(regexp = "^[0-9+ ]{7,20}$", message = "Phone number must contain only digits, spaces or + and be between 7 and 20 characters") String phone,

                @NotBlank(message = "Father name id is required") @Size(min = 5, max = 255, message = "Father name must be between 5 and 255 characters") String fatherName,

                @NotBlank(message = "Mother name id is required") @Size(min = 5, max = 255, message = "Mother name must be between 5 and 255 characters") String motherName,

                @NotBlank(message = "Street is required") @Size(min = 5, max = 255, message = "Street must be between 5 and 255 characters") String street,

                @NotBlank(message = "City is required") @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters") String city,

                String classMaster

) {
}
