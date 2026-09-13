package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import com.moriba.skultem.domain.vo.Level;

public record TimingDTO(String id, String schoolId, String name, boolean isDefault, LocalTime startTime,
                        LocalTime endTime, int periodDuration, int breakDuration, int lunchDuration,
                        List<Level> levels, Instant createdAt, Instant updatedAt) {
}
