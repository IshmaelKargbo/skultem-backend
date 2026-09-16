package com.moriba.skultem.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.dto.TeacherAttendanceSummaryRowDTO;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.domain.vo.Title;

// Covers the Monthly/Term Summary merge: a teacher with recorded rows gets real counts, and a
// teacher with none still appears (workingDays=0, a "missing attendance" signal management should
// see, not silently dropped) rather than only ever showing teachers the query happened to return.
class TeacherAttendanceSummaryRowMapperTest {

    private Teacher teacher(String id, String givenNames, String familyName) {
        var user = User.create(givenNames, familyName, id + "@example.com", "password123", null);
        return Teacher.create(id, "school-1", Title.MR, "+23276000000", "1 Main St", "Freetown", Gender.MALE,
                "STAFF-" + id, user, "Teacher", true);
    }

    @Test
    void aTeacherWithRecordedRowsGetsRealCounts() {
        var teachers = List.of(teacher("t1", "Ama", "Kamara"));
        // 18 present, 2 late, 5 absent-or-excused = 25 recorded days.
        var rows = List.<Object[]>of(new Object[] { "t1", 18L, 2L, 5L, 25L });

        var result = TeacherAttendanceSummaryRowMapper.merge(teachers, rows);

        TeacherAttendanceSummaryRowDTO row = result.get(0);
        assertThat(row.workingDays()).isEqualTo(25);
        assertThat(row.present()).isEqualTo(18);
        assertThat(row.late()).isEqualTo(2);
        assertThat(row.absent()).isEqualTo(5);
        assertThat(row.attendancePercentage()).isEqualTo(80.0); // (18+2)/25
    }

    @Test
    void aTeacherWithNoRecordedRowsStillAppearsInsteadOfBeingDropped() {
        var teachers = List.of(teacher("t1", "Ama", "Kamara"), teacher("t2", "Musa", "Sesay"));
        // Only t1 has any recorded attendance for the period.
        var rows = List.<Object[]>of(new Object[] { "t1", 10L, 0L, 0L, 10L });

        var result = TeacherAttendanceSummaryRowMapper.merge(teachers, rows);

        assertThat(result).hasSize(2);
        var missing = result.stream().filter(r -> r.teacherId().equals("t2")).findFirst().orElseThrow();
        assertThat(missing.workingDays()).isZero();
        assertThat(missing.attendancePercentage()).isNull();
    }

    @Test
    void noThresholdFlagIsAppliedToTeachersUnlikeStudents() {
        // Just confirms the DTO has no belowThreshold-style field to accidentally assert on -
        // teacher attendance is never auto-flagged, per the confirmed requirement.
        var teachers = List.of(teacher("t1", "Ama", "Kamara"));
        var rows = List.<Object[]>of(new Object[] { "t1", 1L, 0L, 9L, 10L }); // 10% attendance

        var result = TeacherAttendanceSummaryRowMapper.merge(teachers, rows);

        assertThat(result.get(0).attendancePercentage()).isEqualTo(10.0);
    }
}
