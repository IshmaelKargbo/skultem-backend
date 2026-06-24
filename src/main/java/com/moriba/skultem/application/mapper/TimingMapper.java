package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.TimingDTO;
import com.moriba.skultem.domain.model.Timing;

public class TimingMapper {
    public static TimingDTO toDTO(Timing param) {
        if (param == null)
            return null;

        return new TimingDTO(param.getId(), param.getSchoolId(), param.getStartTime(), param.getEndTime(), param.getPeriodDuration(), param.getBreakDuration(), param.getLunchDuration(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
