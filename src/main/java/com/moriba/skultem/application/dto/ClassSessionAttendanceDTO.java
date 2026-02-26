package com.moriba.skultem.application.dto;

import java.time.LocalDate;
import java.util.List;

public record ClassSessionAttendanceDTO(
        String classSessionId,
        LocalDate date,
        boolean holiday,
        int totalStudents,
        int markedCount,
        int unmarkedCount,
        int presentCount,
        int absentCount,
        int excusedCount,
        int lateCount,
        List<ClassSessionAttendanceRecordDTO> records) {
}
