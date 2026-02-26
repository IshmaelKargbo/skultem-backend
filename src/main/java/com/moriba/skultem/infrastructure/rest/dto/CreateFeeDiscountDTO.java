package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.moriba.skultem.utils.ValidPercentageValue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@ValidPercentageValue
public record CreateFeeDiscountDTO(
        @NotBlank(message = "Discount kind is required") @Pattern(regexp = "PERCENTAGE|AMOUNT", message = "Kind must be either PERCENTAGE or AMOUNT") String kind,

        @NotBlank(message = "Name is required") String name,

        @NotNull(message = "Value is required") @Positive(message = "Value must be greater than 0") BigDecimal value,

        @NotNull(message = "Expiry date is required") @FutureOrPresent(message = "Expiry date cannot be in the past") LocalDate expiryDate,

        @NotBlank(message = "Student is required") String studentId,

        @NotBlank(message = "Reason is required") String reason,

        @NotBlank(message = "Fee is required") String feeId) {
}