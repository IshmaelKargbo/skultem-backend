package com.moriba.skultem.application.dto;

import java.util.List;

// termLabel is null when the request asked for "All Terms" (the whole academic year), otherwise
// the resolved Term's name.
public record ClassAttendanceSummaryDTO(
        List<ClassAttendanceSummaryRowDTO> classes,
        String termLabel,
        int totalClasses,
        long totalStudents,
        double schoolAverageAttendance,
        int classesBelowThreshold,
        long totalBoys,
        long totalGirls,
        long presentBoys,
        long presentGirls) {
}
