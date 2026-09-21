package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record NationalCalendarDTO(
        String id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean current,
        List<NationalTermDTO> terms,
        Instant updatedAt) {

    public record NationalTermDTO(int termNumber, String name, LocalDate startDate, LocalDate endDate) {
    }
}
