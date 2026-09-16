package com.moriba.skultem.application.mapper;

import java.util.List;

import com.moriba.skultem.application.dto.StudentAttendanceSummaryDTO;
import com.moriba.skultem.domain.service.AttendanceRateCalculator;
import com.moriba.skultem.domain.vo.Gender;

// Shared row-mapping for AttendanceRepository.attendanceCountsByClassAndDateRange's Object[] rows
// (enrollmentId, studentId, givenNames, familyName, admissionNumber, gender, presentOrLateCount,
// lateCount, totalRecorded) - used by both GenerateMonthlyAttendanceSummaryUseCase and
// GenerateTermAttendanceSummaryUseCase so the Present/Absent/Late/percentage math can't drift
// between the two report types. Excused absences fold into "Absent" (no separate column in
// either report, per the confirmed Monthly/Term Summary column set).
public final class AttendanceSummaryRowMapper {

    private AttendanceSummaryRowMapper() {
    }

    public static List<StudentAttendanceSummaryDTO> toStudentSummaries(List<Object[]> rows, String className,
            double attendanceThreshold) {
        return rows.stream().map(row -> {
            String enrollmentId = (String) row[0];
            String studentId = (String) row[1];
            String givenNames = (String) row[2];
            String familyName = (String) row[3];
            String admissionNumber = (String) row[4];
            Gender gender = (Gender) row[5];
            long presentOrLate = ((Number) row[6]).longValue();
            long late = ((Number) row[7]).longValue();
            long totalRecorded = ((Number) row[8]).longValue();

            long presentOnly = presentOrLate - late;
            long absent = totalRecorded - presentOrLate;

            Double percentage = totalRecorded > 0 ? AttendanceRateCalculator.rate(presentOrLate, totalRecorded)
                    : null;
            boolean belowThreshold = AttendanceRateCalculator.isBelowThreshold(percentage, attendanceThreshold);

            return new StudentAttendanceSummaryDTO(
                    studentId,
                    enrollmentId,
                    String.join(" ", givenNames, familyName),
                    admissionNumber,
                    className,
                    gender != null ? gender.name() : null,
                    totalRecorded,
                    presentOnly,
                    absent,
                    late,
                    percentage,
                    belowThreshold);
        }).toList();
    }
}
