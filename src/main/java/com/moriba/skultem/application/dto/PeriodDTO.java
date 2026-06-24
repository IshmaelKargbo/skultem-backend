package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

public record PeriodDTO(String id, String name, String startTime, String endTime, boolean isBreak,
                        boolean isLunch, List<TimetableDTO> subjects, Instant createdAt, Instant updatedAt) {
}
