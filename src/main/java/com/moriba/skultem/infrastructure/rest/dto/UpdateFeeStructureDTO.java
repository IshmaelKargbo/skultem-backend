package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

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

        // One or more materials this fee bundles when hasSupply is true - e.g. a Uniform fee
        // carrying the Uniform itself, a House Colour, and a Necktie, each its own line.
        @Valid
        List<FeeStructureSupplyItemInputDTO> supplyItems,

        @Length(max = 255, message = "Description must not exceed 255 characters")
        String description,

        boolean newStudentsOnly,

        boolean oldStudentsOnly,

        @Pattern(regexp = "MALE|FEMALE", message = "Gender must be MALE or FEMALE")
        String gender

) {

    public UpdateFeeStructureDTO {
        description = normalize(description);
        gender = normalize(gender);

        if (newStudentsOnly && oldStudentsOnly) {
            throw new IllegalArgumentException("newStudentsOnly and oldStudentsOnly cannot both be true");
        }

        if (hasSupply) {
            if (supplyItems == null || supplyItems.isEmpty()) {
                throw new IllegalArgumentException("At least one supply item is required when hasSupply is true");
            }
        } else {
            if (supplyItems != null && !supplyItems.isEmpty()) {
                throw new IllegalArgumentException("supplyItems are not allowed when hasSupply is false");
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
