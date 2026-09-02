package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.model.TeacherAttendance.Status;

// One teacher's attendance status for a given day - `status` is null when the teacher simply
// hasn't been marked yet (as opposed to ABSENT, a real recorded status). clockedInAt/clockedOutAt
// (with their IPs) are set whenever a real clock-in/out happened, either the teacher's own
// geofenced Clock In/Out or an admin clocking them in/out on their behalf - the clockIn/OutByAdmin
// flags tell the roster which one it was, so it can show "self clocked in at 7:42am" vs "clocked
// in by admin at 7:42am".
public record TeacherRosterEntryDTO(
        TeacherDTO teacher,
        Status status,
        String note,
        Instant clockedInAt,
        String clockInIp,
        Instant clockedOutAt,
        String clockOutIp,
        boolean clockInByAdmin,
        boolean clockOutByAdmin) {
}
