package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.LeaveRequest.Status;
import com.moriba.skultem.domain.model.LeaveRequest.Type;

public record LeaveRequestDTO(
        String id,
        String schoolId,
        TeacherDTO teacher,
        Type type,
        LocalDate startDate,
        LocalDate endDate,
        long durationDays,
        String reason,
        Status status,
        String reviewNote,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt) {
}
