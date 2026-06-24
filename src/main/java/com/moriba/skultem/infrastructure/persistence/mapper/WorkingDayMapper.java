package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.WorkingDay;
import com.moriba.skultem.infrastructure.persistence.entity.WorkingDayEntity;

public class WorkingDayMapper {
    public static WorkingDay toDomain(WorkingDayEntity param) {
        var timing = TimingMapper.toDomain(param.getTiming());
        return new WorkingDay(param.getId(), param.getSchoolId(), timing, param.getDay(), param.isState(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static WorkingDayEntity toEntity(WorkingDay param) {
        return WorkingDayEntity.builder()
                .id(param.getId())
                .timing(TimingMapper.toEntity(param.getTiming()))
                .day(param.getDay())
                .state(param.isState())
                .schoolId(param.getSchoolId())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
