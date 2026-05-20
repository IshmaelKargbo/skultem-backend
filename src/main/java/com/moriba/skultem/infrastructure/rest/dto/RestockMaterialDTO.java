package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RestockMaterialDTO(

                @NotBlank(message = "Id is required") String id,

                @NotNull(message = "Stock quantity is required") @Min(value = 1, message = "Stock quantity must be greater than 0") Integer inStock,
                String note) {
}