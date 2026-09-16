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

    public static List<TeacherAttendanceSummaryRowDTO> merge(List<Teacher> activeTeachers, List<Object[]> rows) {
        Map<String, Object[]> byTeacherId = rows.stream()
                .collect(Collectors.toMap(r -> (String) r[0], Function.identity()));

        return activeTeachers.stream().map(teacher -> {
            var row = byTeacherId.get(teacher.getId());

            if (row == null) {
                return new TeacherAttendanceSummaryRowDTO(teacher.getId(), teacher.getName(), 0, 0, 0, 0, null);
            }

            long present = ((Number) row[1]).longValue();
            long late = ((Number) row[2]).longValue();
            long absent = ((Number) row[3]).longValue();
            long total = ((Number) row[4]).longValue();
            Double percentage = total > 0 ? AttendanceRateCalculator.rate(present + late, total) : null;

            return new TeacherAttendanceSummaryRowDTO(teacher.getId(), teacher.getName(), total, present, absent,
                    late, percentage);
        }).toList();
    }
}
