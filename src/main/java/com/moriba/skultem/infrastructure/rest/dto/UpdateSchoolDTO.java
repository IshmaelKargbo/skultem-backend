package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSchoolDTO(
        @NotBlank(message = "School name is required") @Size(min = 3, max = 150, message = "School name must be between 3 and 150 characters") String name,

        @NotBlank(message = "Domain is required") String domain,

        @NotBlank(message = "Street is required") @Size(min = 5, max = 255, message = "Street must be between 5 and 255 characters") String street,

        @NotBlank(message = "Region is required") String region,

        @NotBlank(message = "District is required") String district,

        @NotBlank(message = "Chiefdom is required") @Size(min = 5, max = 255, message = "Chiefdom must be between 5 and 255 characters") String chiefdom,

        @NotBlank(message = "City is required") String city,

        @DecimalMin(value = "0", message = "Attendance threshold cannot be less than 0") @DecimalMax(value = "100", message = "Attendance threshold cannot be greater than 100") Double attendanceThreshold
) {
}
