package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.Owner;

class SchoolAttendanceRulesTest {

    private School school() {
        return School.create("s-1", "Test School", "test",
                new Address("Western Area", "Freetown", "Freetown", "Freetown", "1 Main St"),
                new Owner("Jane", "Doe", "jane@example.com", "+23276000000"));
    }

    @Test
    void aNewSchoolGetsTheDefaults() {
        var rules = school().attendanceRules();
        assertThat(rules.windowDays()).isEqualTo(20);
        assertThat(rules.minDays()).isEqualTo(5);
        assertThat(rules.streakDays()).isEqualTo(3);
        assertThat(rules.threshold()).isEqualTo(75.0);
    }

    @Test
    void nullKeepsTheCurrentValueAndAValueReplacesIt() {
        var s = school();
        s.updateAttendanceRules(30, null, 4);
        var rules = s.attendanceRules();
        assertThat(rules.windowDays()).isEqualTo(30);
        assertThat(rules.minDays()).isEqualTo(5);
        assertThat(rules.streakDays()).isEqualTo(4);
    }

    @Test
    void outOfRangeValuesAreRejectedAndLeaveTheSchoolUnchanged() {
        var s = school();
        assertThatThrownBy(() -> s.updateAttendanceRules(2, null, null)).isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> s.updateAttendanceRules(100, null, null)).isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> s.updateAttendanceRules(10, 11, null)).isInstanceOf(RuleException.class); // min > window
        assertThatThrownBy(() -> s.updateAttendanceRules(null, null, 1)).isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> s.updateAttendanceRules(null, null, 11)).isInstanceOf(RuleException.class);
        assertThat(s.attendanceRules().windowDays()).isEqualTo(20);
    }
}
