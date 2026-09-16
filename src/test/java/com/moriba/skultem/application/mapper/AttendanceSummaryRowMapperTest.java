package com.moriba.skultem.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.dto.StudentAttendanceSummaryDTO;
import com.moriba.skultem.domain.vo.Gender;

// Covers the Present/Absent/Late split and percentage/threshold math shared by Monthly Summary
// and Term Summary - both use cases hand AttendanceRepository.attendanceCountsByClassAndDateRange's
// raw rows straight to this mapper, so its correctness is their correctness.
class AttendanceSummaryRowMapperTest {

    private Object[] row(String enrollmentId, String studentId, String givenNames, String familyName,
            String admissionNumber, Gender gender, long presentOrLate, long late, long totalRecorded) {
        return new Object[] { enrollmentId, studentId, givenNames, familyName, admissionNumber, gender,
                presentOrLate, late, totalRecorded };
    }

    @Test
    void splitsPresentOrLateIntoSeparatePresentAndLateColumnsAndDerivesAbsentFromTheRemainder() {
        // 30 recorded days: 20 present-or-late (of which 5 are late), so 15 plain-present, 5 late,
        // and the remaining 10 (absent + excused, both fold into Absent) make up the rest.
        var rows = List.<Object[]>of(row("e1", "s1", "Ama", "Kamara", "A-001", Gender.FEMALE, 20, 5, 30));

        var result = AttendanceSummaryRowMapper.toStudentSummaries(rows, "Primary 5", 75.0);

        StudentAttendanceSummaryDTO dto = result.get(0);
        assertThat(dto.studentName()).isEqualTo("Ama Kamara");
        assertThat(dto.gender()).isEqualTo("FEMALE");
        assertThat(dto.schoolDays()).isEqualTo(30);
        assertThat(dto.present()).isEqualTo(15);
        assertThat(dto.late()).isEqualTo(5);
        assertThat(dto.absent()).isEqualTo(10);
        assertThat(dto.present() + dto.late() + dto.absent()).isEqualTo(dto.schoolDays());
    }

    @Test
    void attendancePercentageCountsLateAsAttendedConsistentlyWithTheRestOfTheApp() {
        // 27 of 30 present-or-late = 90% - late counts toward the percentage, matching
        // ComputeClassAttentionUseCase's existing "needs attention" convention.
        var rows = List.<Object[]>of(row("e1", "s1", "Ama", "Kamara", "A-001", Gender.FEMALE, 27, 2, 30));

        var result = AttendanceSummaryRowMapper.toStudentSummaries(rows, "Primary 5", 75.0);

        assertThat(result.get(0).attendancePercentage()).isEqualTo(90.0);
        assertThat(result.get(0).belowThreshold()).isFalse();
    }

    @Test
    void flagsAStudentBelowTheConfiguredThresholdButNotOneExactlyAtIt() {
        // 22/30 = 73.3%, below a 75% threshold.
        var below = List.<Object[]>of(row("e1", "s1", "Ama", "Kamara", "A-001", Gender.FEMALE, 22, 0, 30));
        assertThat(AttendanceSummaryRowMapper.toStudentSummaries(below, "Primary 5", 75.0).get(0).belowThreshold())
                .isTrue();

        // Exactly 75.0% must not be flagged (threshold is exclusive).
        var atThreshold = List.<Object[]>of(row("e2", "s2", "Musa", "Sesay", "A-002", Gender.MALE, 15, 0, 20));
        assertThat(AttendanceSummaryRowMapper.toStudentSummaries(atThreshold, "Primary 5", 75.0).get(0)
                .belowThreshold()).isFalse();
    }

    @Test
    void aStudentWithNoRecordedDaysHasANullPercentageAndIsNeverFlagged() {
        var rows = List.<Object[]>of(row("e1", "s1", "Ama", "Kamara", "A-001", Gender.FEMALE, 0, 0, 0));

        var result = AttendanceSummaryRowMapper.toStudentSummaries(rows, "Primary 5", 75.0);

        assertThat(result.get(0).attendancePercentage()).isNull();
        assertThat(result.get(0).belowThreshold()).isFalse();
    }

    @Test
    void aHigherSchoolConfiguredThresholdFlagsAStudentThatALowerThresholdWouldNot() {
        // 78% attendance: fine against a 75% bar, flagged against an 80% bar.
        var rows = List.<Object[]>of(row("e1", "s1", "Ama", "Kamara", "A-001", Gender.MALE, 78, 0, 100));

        assertThat(AttendanceSummaryRowMapper.toStudentSummaries(rows, "Primary 5", 75.0).get(0).belowThreshold())
                .isFalse();
        assertThat(AttendanceSummaryRowMapper.toStudentSummaries(rows, "Primary 5", 80.0).get(0).belowThreshold())
                .isTrue();
    }

    @Test
    void genderPassesThroughAsAPlainStringForGenderBreakdownTotals() {
        var rows = List.<Object[]>of(
                row("e1", "s1", "Ama", "Kamara", "A-001", Gender.FEMALE, 10, 0, 10),
                row("e2", "s2", "Musa", "Sesay", "A-002", Gender.MALE, 10, 0, 10));

        var result = AttendanceSummaryRowMapper.toStudentSummaries(rows, "Primary 5", 75.0);

        assertThat(result).extracting(StudentAttendanceSummaryDTO::gender).containsExactly("FEMALE", "MALE");
    }
}
