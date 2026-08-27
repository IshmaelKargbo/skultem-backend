package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.CalendarEvent;
import com.moriba.skultem.infrastructure.persistence.entity.CalendarEventEntity;

public class CalendarEventMapper {
    public static CalendarEvent toDomain(CalendarEventEntity param) {
        if (param == null) return null;

        return new CalendarEvent(param.getId(), param.getSchoolId(), param.getTitle(), param.getDescription(),
                param.getType(), param.getStartDate(), param.getEndDate(), param.getLocation(),
                param.getCreatedByUserId(), param.getCreatedByName(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static CalendarEventEntity toEntity(CalendarEvent param) {
        if (param == null) return null;

        return CalendarEventEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .title(param.getTitle())
                .description(param.getDescription())
                .type(param.getType())
                .startDate(param.getStartDate())
                .endDate(param.getEndDate())
                .location(param.getLocation())
                .createdByUserId(param.getCreatedByUserId())
                .createdByName(param.getCreatedByName())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
