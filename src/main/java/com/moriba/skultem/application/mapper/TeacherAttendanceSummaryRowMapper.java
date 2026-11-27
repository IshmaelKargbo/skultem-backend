package com.moriba.skultem.application.mapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.moriba.skultem.application.dto.TeacherAttendanceSummaryRowDTO;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.service.AttendanceRateCalculator;

// Merges TeacherAttendanceRepository.attendanceCountsByTeacherAndDateRange's rows against the
// full active-teacher roster - used by both Monthly and Term Summary so the math can't drift
// between them. Starting from the roster (not just the query rows) means a teacher with zero
// recorded days for the period still shows up with workingDays=0 rather than silently vanishing -
// a missing-attendance signal is useful to management, not noise to hide.
public final class TeacherAttendanceSummaryRowMapper {

    private TeacherAttendanceSummaryRowMapper() {
    }

    // schoolDays: the school days in the period so far. Every teacher is measured against all of them - a day
    // with no record counts as absent, so 12 days marked out of 19 is 63.2%, not 100%.
    public static List<TeacherAttendanceSummaryRowDTO> merge(List<Teacher> activeTeachers, List<Object[]> rows,
            long schoolDays) {
        Map<String, Object[]> byTeacherId = rows.stream()
                .collect(Collectors.toMap(r -> (String) r[0], Function.identity()));

        return activeTeachers.stream().map(teacher -> {
            var row = byTeacherId.get(teacher.getId());

            long presentRaw = row == null ? 0 : ((Number) row[1]).longValue();
            long lateRaw = row == null ? 0 : ((Number) row[2]).longValue();

            // Capped: records on a weekend or a closed day can't push anyone past 100%.
            long attended = Math.min(presentRaw + lateRaw, schoolDays);
            long late = Math.min(lateRaw, attended);
            long present = attended - late;
            long absent = schoolDays - attended;
            Double percentage = schoolDays > 0 ? AttendanceRateCalculator.rate(attended, schoolDays) : null;

            return new TeacherAttendanceSummaryRowDTO(teacher.getId(), teacher.getName(), schoolDays, present, absent,
                    late, percentage);
        }).toList();
    }
}
