package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.TimingLevelDTO;
import com.moriba.skultem.domain.model.TimingLevel;

public class TimingLevelMapper {
    public static TimingLevelDTO toDTO(TimingLevel param) {
        return new TimingLevelDTO(param.getLevel(), param.getTiming().getId(), param.getTiming().getName());
    }
}
