package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.model.CalendarEvent.Type;

public record CalendarEventDTO(
        String id,
        String title,
        String description,
        Type type,
        Instant startDate,
        Instant endDate,
        String location) {
}
