package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record FamilyDTO(
                @NotBlank(message = "Father session is required") String fatherName,

                @NotBlank(message = "Mother session is required") String motherName,

                @NotBlank(message = "Father occupation is required") String fatherOccupation,

                @NotBlank(message = "Mother occupation is required") String motherOccupation,

                @NotBlank(message = "Mother contact is required") String motherContact,

                @NotBlank(message = "Father contact required") String fatherContact) {
}