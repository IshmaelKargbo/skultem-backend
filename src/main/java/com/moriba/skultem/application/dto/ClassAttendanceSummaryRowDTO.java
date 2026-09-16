package com.moriba.skultem.application.dto;

// One class session's attendance performance within Class Summary's date range - "SSS 1 Science"
// and "SSS 1 Art" are separate rows, same section/stream boundary as everywhere else attendance
// is reported.
public record ClassAttendanceSummaryRowDTO(
        String classId,
        String sectionId,
        String streamId,
        String className,
        long totalStudents,
        long totalBoys,
        long totalGirls,
        long present,
        long absent,
        long late,
        long presentBoys,
        long presentGirls,
        Double attendancePercentage,
        boolean belowThreshold) {
}
