package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.Owner;

class AttendanceRulesResolverTest {

    private School school() {
        var school = School.create("s-1", "Test School", "test",
                new Address("Western Area", "Freetown", "Freetown", "Freetown", "1 Main St"),
                new Owner("Jane", "Doe", "jane@example.com", "+23276000000"));
        school.update(school.getName(), school.getDomain(), school.getAddress(), 75.0,
                School.GenderComposition.MIXED);
        school.updateAttendanceRules(20, 5, 3);
        return school;
    }

    @Test
    void aSectionWithNoOverridesInheritsEverythingFromTheSchool() {
        var section = ManagementSection.create("s-1", "Primary", 1);
        var rules = AttendanceRulesResolver.merge(school(), section);
        assertThat(rules.threshold()).isEqualTo(75.0);
        assertThat(rules.windowDays()).isEqualTo(20);
        assertThat(rules.minDays()).isEqualTo(5);
        assertThat(rules.streakDays()).isEqualTo(3);
    }

    @Test
    void aSectionOverridesOnlyWhatItSets() {
        var section = ManagementSection.create("s-1", "Secondary", 2);
        section.updateAttendanceRules(85.0, null, null, 2);
        var rules = AttendanceRulesResolver.merge(school(), section);
        assertThat(rules.threshold()).isEqualTo(85.0);
        assertThat(rules.streakDays()).isEqualTo(2);
        assertThat(rules.windowDays()).isEqualTo(20); // inherited
        assertThat(rules.minDays()).isEqualTo(5); // inherited
    }

    @Test
    void theMinimumNeverExceedsTheWindowInForce() {
        // The school's minimum is 5; the section shrinks the window to 5 and asks for 5 -> fine; but a
        // school minimum larger than a section's smaller window is clamped to that window.
        var s = school();
        s.updateAttendanceRules(30, 10, null);
        var section = ManagementSection.create("s-1", "Primary", 1);
        section.updateAttendanceRules(null, 6, null, null);
        assertThat(AttendanceRulesResolver.merge(s, section).minDays()).isEqualTo(6);
    }

    @Test
    void nullClearsAnOverrideAndBadValuesAreRejected() {
        var section = ManagementSection.create("s-1", "Primary", 1);
        section.updateAttendanceRules(85.0, 30, 8, 4);
        section.updateAttendanceRules(null, null, null, null);
        assertThat(AttendanceRulesResolver.merge(school(), section).threshold()).isEqualTo(75.0);

        assertThatThrownBy(() -> section.updateAttendanceRules(120.0, null, null, null))
                .isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> section.updateAttendanceRules(null, 10, 11, null))
                .isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> section.updateAttendanceRules(null, null, null, 1))
                .isInstanceOf(RuleException.class);
    }
}
