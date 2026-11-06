package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.model.Attendance.RollMark;

// Schools that take two roll calls a day only count a student present when they were there at both.
class AttendanceRollCallTest {

    private Attendance blank() {
        return Attendance.create("A-1", "school-1", null, LocalDate.now(), false, false, false, null, false, null);
    }

    @Test
    void presentAtBothRollCallsIsPresentForTheDay() {
        var a = blank();
        a.markRoll(1, RollMark.PRESENT, null, false, "t1");
        a.markRoll(2, RollMark.PRESENT, null, false, "t1");
        assertThat(a.getStatus()).isEqualTo("Present");
    }

    @Test
    void missingEitherRollCallMakesTheDayAbsent() {
        var morningOnly = blank();
        morningOnly.markRoll(1, RollMark.PRESENT, null, false, "t1");
        morningOnly.markRoll(2, RollMark.ABSENT, null, false, "t1");
        assertThat(morningOnly.getStatus()).isEqualTo("Absent");

        var afternoonOnly = blank();
        afternoonOnly.markRoll(1, RollMark.ABSENT, null, false, "t1");
        afternoonOnly.markRoll(2, RollMark.PRESENT, null, false, "t1");
        assertThat(afternoonOnly.getStatus()).isEqualTo("Absent");
    }

    @Test
    void lateAtOneRollCallAndPresentAtTheOtherIsLate() {
        var a = blank();
        a.markRoll(1, RollMark.LATE, null, false, "t1");
        a.markRoll(2, RollMark.PRESENT, null, false, "t1");
        assertThat(a.getStatus()).isEqualTo("Late");
    }

    @Test
    void excusedAtOneRollCallKeepsItsReason() {
        var a = blank();
        a.markRoll(1, RollMark.PRESENT, null, false, "t1");
        a.markRoll(2, RollMark.EXCUSED, "Clinic visit", false, "t1");
        assertThat(a.getStatus()).isEqualTo("Excused");
        assertThat(a.getReason()).isEqualTo("Clinic visit");
    }

    @Test
    void beforeTheSecondRollCallTheDayFollowsTheFirst() {
        var a = blank();
        a.markRoll(1, RollMark.PRESENT, null, false, "t1");
        assertThat(a.getStatus()).isEqualTo("Present");
        assertThat(a.getSecondRoll()).isNull();
    }

    @Test
    void correctingARollCallRecomputesTheDay() {
        var a = blank();
        a.markRoll(1, RollMark.PRESENT, null, false, "t1");
        a.markRoll(2, RollMark.ABSENT, null, false, "t1");
        a.markRoll(2, RollMark.PRESENT, null, false, "t1");
        assertThat(a.getStatus()).isEqualTo("Present");
    }

    @Test
    void markingTheWholeDayClearsRollCallMarks() {
        var a = blank();
        a.markRoll(1, RollMark.ABSENT, null, false, "t1");
        a.update(true, false, false, null, false, "admin");
        assertThat(a.getFirstRoll()).isNull();
        assertThat(a.getStatus()).isEqualTo("Present");
    }
}
