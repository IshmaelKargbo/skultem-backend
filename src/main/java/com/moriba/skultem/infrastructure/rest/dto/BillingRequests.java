package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// The request bodies for subscriptions and billing.
public final class BillingRequests {

    private BillingRequests() {
    }

    public record SavePlan(
            @NotBlank(message = "Plan name is required") @Size(max = 120, message = "Plan name must be at most 120 characters") String name,
            @Size(max = 2000) String description,
            @NotNull(message = "Price is required") @DecimalMin(value = "0", message = "Price cannot be negative") @Digits(integer = 12, fraction = 2, message = "Price can have at most 2 decimal places") BigDecimal price,
            @NotBlank(message = "Billing frequency is required") @Pattern(regexp = "MONTHLY|QUARTERLY|ANNUAL", message = "Frequency must be MONTHLY, QUARTERLY or ANNUAL") String frequency,
            @Positive(message = "Student limit must be more than 0") Integer maxStudents,
            boolean active) {
    }

    public record CreateSubscription(
            @NotBlank(message = "School is required") String schoolId,
            @NotBlank(message = "Plan is required") String planId,
            @NotNull(message = "Start date is required") LocalDate startDate,
            // The agreed price when it differs from the plan's; blank = the plan's price.
            @DecimalMin(value = "0", message = "Price cannot be negative") @Digits(integer = 12, fraction = 2) BigDecimal price,
            @Size(max = 2000) String notes) {
    }

    public record UpdateSubscription(
            @NotNull(message = "Price is required") @DecimalMin(value = "0", message = "Price cannot be negative") @Digits(integer = 12, fraction = 2) BigDecimal price,
            @Size(max = 2000) String notes) {
    }

    public record RecordPayment(
            @NotNull(message = "Amount is required") @Positive(message = "Amount must be more than 0") @Digits(integer = 12, fraction = 2, message = "Amount can have at most 2 decimal places") BigDecimal amount,
            @NotBlank(message = "Payment method is required") @Pattern(regexp = "CASH|BANK_TRANSFER|MOBILE_MONEY|OTHER", message = "Unknown payment method") String method,
            @Size(max = 255, message = "Reference must be at most 255 characters") String reference,
            Instant paidAt,
            @Size(max = 2000) String note) {
    }

    public record Reason(@NotBlank(message = "Give a reason") @Size(max = 2000) String reason) {
    }
}
