package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.PeriodDTO;
import com.moriba.skultem.application.dto.TimetableDTO;
import com.moriba.skultem.domain.model.Period;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PeriodMapper {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    public static PeriodDTO toDTO(Period param, List<TimetableDTO> subjects) {
        if (param == null) {
            return null;
        }

        return new PeriodDTO(
                param.getId(),
                param.getName(),
                param.getStartTime().format(TIME_FORMAT),
                param.getEndTime().format(TIME_FORMAT),
                param.isBreak(),
                param.isLunch(),
                subjects,
                param.getCreatedAt(),
                param.getUpdatedAt()
        );
    }
}