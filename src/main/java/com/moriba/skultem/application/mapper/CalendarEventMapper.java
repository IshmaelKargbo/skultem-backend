package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.CalendarEventDTO;
import com.moriba.skultem.domain.model.CalendarEvent;

public class CalendarEventMapper {
    public static CalendarEventDTO toDTO(CalendarEvent param) {
        return new CalendarEventDTO(
                param.getId(),
                param.getTitle(),
                param.getDescription(),
                param.getType(),
                param.getStartDate(),
                param.getEndDate(),
                param.getLocation());
    }
}
