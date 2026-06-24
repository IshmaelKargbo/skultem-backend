package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalTime;

public record TimingDTO(String id, String schoolId, LocalTime startTime, LocalTime endTime, int periodDuration,
                        int breakDuration, int lunchDuration, Instant createdAt, Instant updatedAt) {
}
