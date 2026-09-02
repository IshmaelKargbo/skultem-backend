package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.moriba.skultem.domain.model.TeacherAttendance.Status;

// One day in a single teacher's own attendance history - powers the calendar on their profile
// page. No `teacher` field (unlike TeacherRosterEntryDTO) since the endpoint is already scoped to
// one teacher.
public record TeacherAttendanceDayDTO(
        LocalDate date,
        Status status,
        String note,
        Instant clockedInAt,
        Instant clockedOutAt,
        boolean clockInByAdmin,
        boolean clockOutByAdmin) {
}
