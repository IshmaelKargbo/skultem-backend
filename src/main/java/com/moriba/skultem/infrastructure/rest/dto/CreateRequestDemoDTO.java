package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRequestDemoDTO(
                @NotBlank(message = "Full name is required") @Size(min = 3, max = 150, message = "Full name must be between 3 and 150 characters") String name,

                @NotBlank(message = "School is required") String school,

                @NotBlank(message = "Address is required") @Size(min = 5, max = 255, message = "Street must be between 5 and 255 characters") String address,

                @NotBlank(message = "City is required") String city,

                @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

                @NotBlank(message = "Phone is required") @Pattern(regexp = "^[0-9+]{7,15}$", message = "Phone number must be between 7 and 15 digits and may include +") String phone,

                @NotBlank(message = "Preferred walkthrough format are required") String preferred,

                @NotBlank(message = "Main priority is required") String priority,

                String message) {
}
