package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateMaterialSaleDTO(

        @NotBlank(message = "Material is required") String materialId,

        // Optional - a sale can be linked to an enrolled student, or, for a walk-in buyer, carry
        // just a customerName instead. Exactly one of the two must be present (checked below).
        String studentId,

        @Length(max = 150, message = "Customer name must not exceed 150 characters")
        String customerName,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Unit price is required")
        @PositiveOrZero(message = "Unit price cannot be negative")
        BigDecimal unitPrice,

        // Defaults to 0 (unpaid) when omitted - lets a pre-sale be recorded before any money
        // changes hands, with the balance settled later via the payment endpoint.
        @PositiveOrZero(message = "Amount paid cannot be negative")
        BigDecimal amountPaid,

        @Pattern(regexp = "CASH|BANK|MOBILE_MONEY", message = "Payment method must be CASH, BANK or MOBILE_MONEY")
        String paymentMethod,

        @Length(max = 255, message = "Note must not exceed 255 characters")
        String note,

        // Whether the buyer is taking the item away right now - independent of whether it's in
        // stock: a parent can pay today and still ask to collect later, even for something sitting
        // on the shelf. Defaults to true (the common walk-up-and-buy case) when omitted; only
        // matters when stock is actually available - an out-of-stock item is a pre-sale regardless.
        Boolean collectNow) {

    public CreateMaterialSaleDTO {
        boolean hasStudent = studentId != null && !studentId.isBlank();
        boolean hasCustomerName = customerName != null && !customerName.isBlank();

        if (!hasStudent && !hasCustomerName) {
            throw new IllegalArgumentException("Provide either a student or a customer name for this sale");
        }

        if (collectNow == null) collectNow = true;
    }
}
