package com.moriba.skultem.infrastructure.rest.dto;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateMaterialDTO(

        @NotBlank(message = "Name is required") @Length(max = 100, message = "Name must not exceed 100 characters") String name,

        @NotBlank(message = "Unit is required") @Pattern(regexp = "PCS|BOX|PACK|LITRE", message = "Unit must be PCS, BOX, PACK or LITRE") String unit,

        @NotNull(message = "Stock quantity is required") @Min(value = 0, message = "Stock quantity cannot be negative") Integer inStock,

        @NotBlank(message = "Category id is required") String categoryId) {
}