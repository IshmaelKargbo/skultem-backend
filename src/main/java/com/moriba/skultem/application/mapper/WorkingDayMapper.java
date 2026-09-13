package com.moriba.skultem.application.mapper;

import java.util.List;

import com.moriba.skultem.application.dto.WorkingDayDTO;
import com.moriba.skultem.domain.model.WorkingDay;

public class WorkingDayMapper {
    public static WorkingDayDTO toDTO(WorkingDay param) {
        if (param == null)
            return null;

        // Level assignments aren't relevant here - a working day just needs to say which Timing
        // template it belongs to.
        var time = TimingMapper.toDTO(param.getTiming(), List.of());
        return new WorkingDayDTO(param.getId(), param.getDay(), time, param.isState(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
