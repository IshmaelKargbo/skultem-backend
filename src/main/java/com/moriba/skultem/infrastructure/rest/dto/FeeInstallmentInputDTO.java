package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// One part of a fee's installment plan.
public record FeeInstallmentInputDTO(
        @Size(max = 100, message = "Keep installment names under 100 characters") String label,
        @NotNull(message = "Each installment needs an amount") @Positive(message = "Each installment needs an amount above zero") BigDecimal amount,
        @NotNull(message = "Each installment needs a due date") LocalDate dueDate) {

    public com.moriba.skultem.domain.vo.FeeInstallment toDomain() {
        return new com.moriba.skultem.domain.vo.FeeInstallment(label, amount, dueDate);
    }
}
