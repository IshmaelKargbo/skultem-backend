package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.WeekDTO;
import com.moriba.skultem.domain.model.Week;

public class WeekMapper {
    public static WeekDTO toDTO(Week param) {
        if (param == null)
            return null;

        return new WeekDTO(param.getId(), param.getWeek(), param.getTopic(), param.getSubTopic(), param.getObjectives(),
                param.getState().name(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
