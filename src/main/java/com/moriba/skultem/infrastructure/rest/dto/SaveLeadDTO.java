package com.moriba.skultem.infrastructure.rest.dto;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveLeadDTO(
        @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must be at most 255 characters") String name,
        @Size(max = 255, message = "School name must be at most 255 characters") String schoolName,
        @Email(message = "Invalid email format") @Size(max = 255) String email,
        @Pattern(regexp = "^$|^[0-9+ ]{7,20}$", message = "Phone number may only contain digits, spaces and +") String phone,
        @Size(max = 255) String city,
        @Pattern(regexp = "^$|MANUAL|REFERRAL|OTHER|DEMO_REQUEST|CONTACT_MESSAGE", message = "Unknown source") String source,
        @Size(max = 5000, message = "Summary must be at most 5000 characters") String summary,
        String assignedToId,
        Instant nextFollowUpAt) {
}
