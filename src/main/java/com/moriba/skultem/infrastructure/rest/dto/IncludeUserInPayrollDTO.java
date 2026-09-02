package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record IncludeUserInPayrollDTO(
        @NotBlank(message = "Staff ID is required") String staffId,

        @NotBlank(message = "Phone is required") String phone,

        @NotBlank(message = "Street is required") String street,

        @NotBlank(message = "City is required") String city,

        @NotBlank(message = "Gender is required") String gender,

        @NotBlank(message = "Title is required") String title,

        String designation) {
}
