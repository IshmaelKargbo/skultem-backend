package com.moriba.skultem.infrastructure.rest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePayrollRunDTO(

        @NotBlank(message = "Period is required")
        String period,

        @NotNull(message = "Pay date is required")
        LocalDate payDate

) {
}
