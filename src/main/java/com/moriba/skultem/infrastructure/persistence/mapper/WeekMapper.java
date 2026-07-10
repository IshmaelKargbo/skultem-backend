package com.moriba.skultem.infrastructure.persistence.mapper;

import java.util.List;

import com.moriba.skultem.domain.model.Week;
import com.moriba.skultem.infrastructure.persistence.entity.WeekEntity;

public class WeekMapper {
    public static Week toDomain(WeekEntity param) {
        var scheme = SchemeOfWorkMapper.toDomain(param.getScheme());
        List<String> objectives = JsonMapper.fromJsonStringList(param.getObjectives());

        return new Week(param.getId(), param.getSchoolId(), param.getWeek(), param.getTopic(), param.getSubtopic(),
                objectives, scheme, param.getState(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static WeekEntity toEntity(Week param) {
        String objectives = JsonMapper.toJson(param.getObjectives());

        return WeekEntity.builder()
                .id(param.getId())
                .scheme(SchemeOfWorkMapper.toEntity(param.getScheme()))
                .objectives(objectives)
                .topic(param.getTopic())
                .subtopic(param.getSubTopic())
                .week(param.getWeek())
                .state(param.getState())
                .schoolId(param.getSchoolId())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
