package com.moriba.skultem.application.mapper;

import java.util.List;

import com.moriba.skultem.application.dto.TimingDTO;
import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.vo.Level;

public class TimingMapper {
    public static TimingDTO toDTO(Timing param, List<Level> levels) {
        if (param == null)
            return null;

        return new TimingDTO(param.getId(), param.getSchoolId(), param.getName(), param.isDefault(),
                param.getStartTime(), param.getEndTime(), param.getPeriodDuration(), param.getBreakDuration(),
                param.getLunchDuration(), levels, param.getCreatedAt(), param.getUpdatedAt());
    }
}
