package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;

public record RecordPaymentDTO(

        @NotBlank(message = "Student is required")
        String studentId,

        @NotBlank(message = "Fee is required")
        String feeId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 12, fraction = 2, message = "Invalid money format")
        BigDecimal amount,

        @NotBlank(message = "Payment method is required")
        @Pattern(
            regexp = "CASH|BANK|MOBILE_MONEY", 
            message = "Payment method must be one of: CASH, BANK, MOBILE_MONEY"
        )
        String method,

        String referenceNo,

        String note
) {
}