package com.moriba.skultem.application.dto;

public record ClassSessionAttendanceRecordDTO(
        String attendanceId,
        String enrollmentId,
        String studentId,
        String admissionNumber,
        String studentName,
        boolean marked,
        boolean holiday,
        boolean present,
        boolean excused,
        boolean late,
        String reason) {
}
