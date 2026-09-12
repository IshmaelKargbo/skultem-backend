package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateMaterialDTO(

        @NotBlank(message = "Name is required") @Length(max = 100, message = "Name must not exceed 100 characters") String name,

        @NotBlank(message = "Unit is required") @Pattern(regexp = "PCS|BOX|PACK|LITRE", message = "Unit must be PCS, BOX, PACK or LITRE") String unit,

        @NotNull(message = "Price is required")
        @PositiveOrZero(message = "Price cannot be negative")
        BigDecimal price,

        @NotBlank(message = "Category id is required") String categoryId) {
}
