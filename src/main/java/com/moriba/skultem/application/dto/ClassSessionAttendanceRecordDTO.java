package com.moriba.skultem.application.dto;

import java.time.Instant;

public record ClassSessionAttendanceRecordDTO(
        String attendanceId,
        String enrollmentId,
        String studentId,
        String admissionNumber,
        String studentName,
        String gender,
        String photo,
        boolean marked,
        boolean holiday,
        boolean present,
        boolean excused,
        boolean late,
        String reason,
        // Null for a record marked before recordedByUserId was introduced, or for an unmarked
        // student placeholder - the Daily Register renders "-" for either case rather than
        // fabricating a value.
        String recordedBy,
        Instant recordedAt) {
}
