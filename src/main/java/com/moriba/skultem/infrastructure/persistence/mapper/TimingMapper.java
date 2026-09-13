package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.infrastructure.persistence.entity.TimingEntity;

public class TimingMapper {
    public static Timing toDomain(TimingEntity param) {
        return new Timing(param.getId(), param.getSchoolId(), param.getName(), param.isDefault(),
                param.getStartTime(), param.getEndTime(), param.getPeriodDuration(), param.getBreakDuration(),
                param.getLunchDuration(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static TimingEntity toEntity(Timing param) {
        return TimingEntity.builder()
                .id(param.getId())
                .name(param.getName())
                .isDefault(param.isDefault())
                .startTime(param.getStartTime())
                .endTime(param.getEndTime())
                .periodDuration(param.getPeriodDuration())
                .breakDuration(param.getBreakDuration())
                .lunchDuration(param.getLunchDuration())
                .schoolId(param.getSchoolId())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
