package com.moriba.skultem.application.dto;

import java.time.LocalDate;
import java.util.List;

public record TeacherAttendanceRosterDTO(
        LocalDate date,
        List<TeacherRosterEntryDTO> entries,
        long presentCount,
        long lateCount,
        long absentCount,
        long excusedCount,
        long unmarkedCount,
        long totalCount,
        double rate) {
}
