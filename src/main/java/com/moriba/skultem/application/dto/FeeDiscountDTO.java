package com.moriba.skultem.application.dto;

import java.time.LocalDate;
import java.util.List;

// One discount as the user created it: id is the group id, covering every fee in fees.
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
        String totalSaved,
        List<Fee> fees) {

    public record Fee(String feeName, String term, String value, String saved) {
    }
}
