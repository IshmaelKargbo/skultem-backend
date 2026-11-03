package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.model.TeacherAttendance.Status;

// onPayroll: the person has an active staff record at this school. Without one there's nothing to clock in to, so the
// dashboard hides Clock In rather than showing an error.
public record MyAttendanceTodayDTO(Status status, Instant clockedInAt, Instant clockedOutAt, boolean onPayroll) {

    public MyAttendanceTodayDTO(Status status, Instant clockedInAt, Instant clockedOutAt) {
        this(status, clockedInAt, clockedOutAt, true);
    }

    public static MyAttendanceTodayDTO notOnPayroll() {
        return new MyAttendanceTodayDTO(null, null, null, false);
    }
}
