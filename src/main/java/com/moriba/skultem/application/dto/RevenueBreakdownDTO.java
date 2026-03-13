package com.moriba.skultem.application.dto;

public record RevenueBreakdownDTO(
        String label,
        int value,
        int percent
) {}