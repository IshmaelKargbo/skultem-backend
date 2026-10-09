package com.moriba.skultem.application.dto;

import java.time.LocalDate;
import java.util.List;

// Result of GenerateGenderAttendanceSummaryUseCase: boys/girls present for a week, month, term or
// academic year, for the whole school (totals) and per class (classes). "Present" counts are
// present-or-late student-days; percentages are over recorded non-holiday student-days.
public record GenderAttendanceSummaryDTO(
        String period,
        LocalDate startDate,
        LocalDate endDate,
        GenderAttendanceTotalsDTO totals,
        List<ClassGenderAttendanceDTO> classes) {

    public record GenderAttendanceTotalsDTO(
            long boysPresent,
            long girlsPresent,
            long boysRecorded,
            long girlsRecorded,
            double boysAttendancePercentage,
            double girlsAttendancePercentage,
            double overallAttendancePercentage) {
    }

    public record ClassGenderAttendanceDTO(
            String classId,
            String className,
            GenderAttendanceTotalsDTO totals) {
    }
}
