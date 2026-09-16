package com.moriba.skultem.application.dto;

import java.util.List;

public record TermTeacherAttendanceSummaryDTO(
        List<TeacherAttendanceSummaryRowDTO> teachers,
        String termLabel,
        int totalTeachers,
        double averageAttendance,
        long totalPresent,
        long totalAbsent,
        long totalLate) {
}
