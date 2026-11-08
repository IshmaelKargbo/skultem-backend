package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.PeriodDTO;
import com.moriba.skultem.application.dto.TimetableDTO;
import com.moriba.skultem.domain.model.Period;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PeriodMapper {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    public static PeriodDTO toDTO(Period param, List<List<TimetableDTO>> slots) {
        if (param == null) {
            return null;
        }
        // No slots (a deleted period, a break) - an empty grid row, not a crash.
        if (slots == null) {
            slots = List.of();
        }

        return new PeriodDTO(
                param.getId(),
                param.getName(),
                param.getStartTime().format(TIME_FORMAT),
                param.getEndTime().format(TIME_FORMAT),
                param.isBreak(),
                param.isLunch(),
                slots.stream().map(s -> s.isEmpty() ? null : s.get(0)).toList(),
                param.getCreatedAt(),
                param.getUpdatedAt(),
                slots,
                param.isAssembly()
        );
    }
}