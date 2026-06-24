package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Period;
import com.moriba.skultem.domain.model.Room;
import com.moriba.skultem.infrastructure.persistence.entity.PeriodEntity;
import com.moriba.skultem.infrastructure.persistence.entity.RoomEntity;

public class PeriodMapper {
    public static Period toDomain(PeriodEntity param) {
        var session = ClassSessionMapper.toDomain(param.getSession());
        return new Period(param.getId(), param.getSchoolId(), session, param.getType(), param.getName(), param.getStartTime(), param.getEndTime(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static PeriodEntity toEntity(Period param) {
        return PeriodEntity.builder()
                .id(param.getId())
                .type(param.getType())
                .name(param.getName())
                .session(ClassSessionMapper.toEntity(param.getSession()))
                .startTime(param.getStartTime())
                .endTime(param.getEndTime())
                .schoolId(param.getSchoolId())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
