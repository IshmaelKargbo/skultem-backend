package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record Dataset(
        String label,
        List<BigDecimal> data
) {}