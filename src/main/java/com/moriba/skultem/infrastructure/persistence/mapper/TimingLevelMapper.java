package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.model.TimingLevel;
import com.moriba.skultem.infrastructure.persistence.entity.TimingEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TimingLevelEntity;

public class TimingLevelMapper {
    public static TimingLevel toDomain(TimingLevelEntity param) {
        Timing timing = param.getTiming() != null ? TimingMapper.toDomain(param.getTiming()) : null;

        return new TimingLevel(param.getId(), param.getSchoolId(), timing, param.getLevel(), param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static TimingLevelEntity toEntity(TimingLevel args) {
        TimingEntity timing = args.getTiming() != null ? TimingMapper.toEntity(args.getTiming()) : null;

        return TimingLevelEntity.builder()
                .id(args.getId())
                .schoolId(args.getSchoolId())
                .timing(timing)
                .level(args.getLevel())
                .createdAt(args.getCreatedAt())
                .updatedAt(args.getUpdatedAt())
                .build();
    }
}
