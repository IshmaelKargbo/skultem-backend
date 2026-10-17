package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// The marketing site's contact form: full name, work email, school name, phone number, main challenge.
public record CreateContactMessageDTO(
        @NotBlank(message = "Full name is required") @Size(max = 150, message = "Full name must be at most 150 characters") String name,
        @NotBlank(message = "Work email is required") @Email(message = "Invalid email format") @Size(max = 255) String email,
        @NotBlank(message = "School name is required") @Size(max = 255, message = "School name must be at most 255 characters") String school,
        @NotBlank(message = "Phone number is required") @Pattern(regexp = "^[0-9+]{7,15}$", message = "Phone number must be between 7 and 15 digits and may include +") String phone,
        @NotBlank(message = "Please tell us your main challenge") @Size(max = 5000, message = "Main challenge must be at most 5000 characters") String challenge,
        // Honeypot: a hidden field on the marketing form that people leave empty and bots fill in.
        String website) {
}
