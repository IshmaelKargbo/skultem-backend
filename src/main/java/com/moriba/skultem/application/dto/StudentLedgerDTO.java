package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentLedgerDTO(
                LocalDate date,
                String type,
                String student,
                String clazz,
                String description,
                BigDecimal debit,
                BigDecimal credit,
                BigDecimal balance) {
}
