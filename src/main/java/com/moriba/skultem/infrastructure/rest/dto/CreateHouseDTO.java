package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateHouseDTO(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Motto is required")
        @Size(max = 255, message = "Motto must not exceed 255 characters")
        String motto,

        @NotBlank(message = "Color is required")
        @Pattern(
            regexp = "^#([A-Fa-f0-9]{6})$",
            message = "Color must be a valid hex code (e.g. #FF0000)"
        )
        String color,

        @NotEmpty(message = "At least one house master is required")
        List<String> masters

) {
}