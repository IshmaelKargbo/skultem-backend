package com.moriba.skultem.application.dto;

import com.moriba.skultem.domain.model.WorkingDay;

import java.time.Instant;

public record WorkingDayDTO(String id, WorkingDay.Day day, TimingDTO time, boolean state, Instant createdAt, Instant updatedAt) {
}
