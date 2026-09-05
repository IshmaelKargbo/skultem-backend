package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

// One material line for a supply fee - e.g. a Uniform fee bundling Uniform + House Colour +
// Necktie, each its own materialId/quantity pair.
public record FeeStructureSupplyItemInputDTO(

        @NotBlank(message = "Material is required")
        String materialId,

        @Positive(message = "Quantity must be greater than 0")
        int quantity

) {
}
