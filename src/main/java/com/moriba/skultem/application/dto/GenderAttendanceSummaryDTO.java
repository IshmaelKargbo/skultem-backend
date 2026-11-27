package com.moriba.skultem.application.dto;

import java.time.LocalDate;
import java.util.List;

// Result of GenerateGenderAttendanceSummaryUseCase: boys/girls present for a week, month, term or
// academic year, for the whole school (totals) and per class (classes - one row per class + section
// + stream, stream null when the class has none). "Present" counts are
// present-or-late student-days; "recorded" is the student-days expected - every enrolled student
// times the school days in the period so far - and the percentages are over those.
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
            String sectionId,
            String sectionName,
            String streamId,
            String streamName,
            GenderAttendanceTotalsDTO totals) {
    }
}
