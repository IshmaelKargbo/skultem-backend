package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.model.TeacherAttendance.Status;

public record MyAttendanceTodayDTO(Status status, Instant clockedInAt, Instant clockedOutAt) {
}
