package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateFeeStructureDTO(

        @NotBlank(message = "Fee category is required")
        String feeCategory,

        @NotBlank(message = "Term is required")
        String termId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Due date is required")
        LocalDate dueDate,

        boolean allowInstallment,

        boolean hasSupply,

        @PositiveOrZero(message = "Total supply cannot be negative")
        int totalSupply,

        String materialId,

        @Length(max = 255, message = "Description must not exceed 255 characters")
        String description

) {

    public UpdateFeeStructureDTO {
        materialId = normalize(materialId);
        description = normalize(description);

        if (hasSupply) {
            if (materialId == null) {
                throw new IllegalArgumentException("materialId is required when hasSupply is true");
            }
            if (totalSupply <= 0) {
                throw new IllegalArgumentException("totalSupply must be greater than zero");
            }
        } else {
            if (totalSupply > 0) {
                throw new IllegalArgumentException("totalSupply must be zero when hasSupply is false");
            }
            if (materialId != null) {
                throw new IllegalArgumentException("materialId is not allowed when hasSupply is false");
            }
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
