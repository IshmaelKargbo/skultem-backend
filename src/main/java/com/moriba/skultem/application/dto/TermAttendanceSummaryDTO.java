package com.moriba.skultem.application.dto;

import java.util.List;

public record TermAttendanceSummaryDTO(
        List<StudentAttendanceSummaryDTO> students,
        int totalStudents,
        double averageAttendance,
        int studentsBelowThreshold,
        long totalPresent,
        long totalAbsent,
        long totalLate,
        long totalBoys,
        long totalGirls,
        long presentBoys,
        long presentGirls) {
}
