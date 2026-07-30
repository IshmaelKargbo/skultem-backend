package com.moriba.skultem.application.dto;

public record ClassSessionAttendanceRecordDTO(
        String attendanceId,
        String enrollmentId,
        String studentId,
        String admissionNumber,
        String studentName,
        String photo,
        boolean marked,
        boolean holiday,
        boolean present,
        boolean excused,
        boolean late,
        String reason) {
}
