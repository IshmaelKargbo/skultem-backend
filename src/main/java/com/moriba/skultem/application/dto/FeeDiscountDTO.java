package com.moriba.skultem.application.dto;

import java.time.LocalDate;

public record FeeDiscountDTO(
        String id,
        String name,
        String reason,
        String student,
        String clazz,
        String type,
        String value,
        LocalDate applied,
        LocalDate expires,
        String totalSaved) {
}