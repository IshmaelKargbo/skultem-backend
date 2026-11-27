package com.moriba.skultem.application.mapper;

import java.util.List;

import com.moriba.skultem.application.dto.StudentAttendanceSummaryDTO;
import com.moriba.skultem.domain.model.Enrollment;
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

    // The query's rows plus an all-zero row for each enrolled student it has nothing for - a student never
    // marked in the period still appears, missing every school day, rather than silently dropping off.
    public static List<Object[]> withUnmarked(List<Object[]> rows, List<Enrollment> enrolled) {
        var seen = new java.util.HashSet<String>();
        rows.forEach(r -> seen.add((String) r[0]));
        var all = new java.util.ArrayList<>(rows);
        for (Enrollment e : enrolled) {
            if (e.getStudent() == null || !seen.add(e.getId())) continue;
            var s = e.getStudent();
            all.add(new Object[] { e.getId(), s.getId(), s.getGivenNames(), s.getFamilyName(), s.getAdmissionNumber(),
                    s.getGender(), 0L, 0L, 0L });
        }
        return all;
    }

    // schoolDays: the school days in the period so far (ResolveTermSchoolDaysUseCase#between). Everyone is
    // measured against all of them, like the students list: a day with no mark counts as missed, not skipped.
    public static List<StudentAttendanceSummaryDTO> toStudentSummaries(List<Object[]> rows, String className,
            double attendanceThreshold, long schoolDays) {
        return rows.stream().map(row -> {
            String enrollmentId = (String) row[0];
            String studentId = (String) row[1];
            String givenNames = (String) row[2];
            String familyName = (String) row[3];
            String admissionNumber = (String) row[4];
            Gender gender = (Gender) row[5];
            long presentOrLate = ((Number) row[6]).longValue();
            long late = ((Number) row[7]).longValue();
            // Capped: a record on a day later declared closed can't push anyone past 100%.
            long attended = Math.min(presentOrLate, schoolDays);
            long lateDays = Math.min(late, attended);
            long presentOnly = attended - lateDays;
            long absent = schoolDays - attended;

            Double percentage = schoolDays > 0 ? AttendanceRateCalculator.rate(attended, schoolDays) : null;
            boolean belowThreshold = AttendanceRateCalculator.isBelowThreshold(percentage, attendanceThreshold);

            return new StudentAttendanceSummaryDTO(
                    studentId,
                    enrollmentId,
                    String.join(" ", givenNames, familyName),
                    admissionNumber,
                    className,
                    gender != null ? gender.name() : null,
                    schoolDays,
                    presentOnly,
                    absent,
                    lateDays,
                    percentage,
                    belowThreshold,
                    attendanceThreshold);
        }).toList();
    }
}
