package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.WorkingDayDTO;
import com.moriba.skultem.domain.model.WorkingDay;

public class WorkingDayMapper {
    public static WorkingDayDTO toDTO(WorkingDay param) {
        if (param == null)
            return null;

        var time = TimingMapper.toDTO(param.getTiming());
        return new WorkingDayDTO(param.getId(), param.getDay(), time, param.isState(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
