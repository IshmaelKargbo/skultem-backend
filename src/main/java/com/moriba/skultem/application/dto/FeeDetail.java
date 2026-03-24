package com.moriba.skultem.application.dto;

import java.math.BigDecimal;

public record FeeDetail(
        BigDecimal total,
        BigDecimal paid,
        BigDecimal balance,
        String status
) {}