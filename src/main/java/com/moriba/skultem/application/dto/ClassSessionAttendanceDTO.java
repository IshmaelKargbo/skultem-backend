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
        int totalBoys,
        int totalGirls,
        int presentBoys,
        int presentGirls,
        List<ClassSessionAttendanceRecordDTO> records,
        // 1 or 2 roll calls a day for this class, and which one the records show (null = the day).
        int rollCalls,
        Integer roll) {
}
