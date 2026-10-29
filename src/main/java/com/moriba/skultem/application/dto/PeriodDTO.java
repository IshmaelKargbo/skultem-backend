package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

public record PeriodDTO(String id, String name, String startTime, String endTime, boolean isBreak,
                        boolean isLunch, List<TimetableDTO> subjects, Instant createdAt, Instant updatedAt,
                        // One list per working day: every subject in that slot. More than one means students
                        // split between options (e.g. PHE or RME). `subjects` keeps the first of each, for
                        // older screens.
                        List<List<TimetableDTO>> slots) {
}
