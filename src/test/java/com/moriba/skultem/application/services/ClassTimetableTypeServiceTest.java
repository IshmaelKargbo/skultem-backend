package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.ClassTimetableTypeService.Type;

// A class runs on the normal timetable or the weekly plan, never both.
class ClassTimetableTypeServiceTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ClassTimetableTypeService types = new ClassTimetableTypeService(jdbc);

    // The type stored for class section "CS1" (null: the class has no choice saved).
    private void sessionType(String type) {
        when(jdbc.queryForList(contains("FROM class_sessions cs"), eq(String.class), eq("CS1"), eq("S1")))
                .thenReturn(Arrays.asList(type));
    }

    @Test
    void aClassWithNoChoiceRunsOnTheNormalTimetable() {
        sessionType(null);
        assertThat(types.typeOfSession("S1", "CS1")).isEqualTo(Type.PERIODS);
        assertThatCode(() -> types.requirePeriods("S1", "CS1")).doesNotThrowAnyException();
        assertThatThrownBy(() -> types.requireWeeklyPlan("S1", "CS1")).isInstanceOf(RuleException.class)
                .hasMessageContaining("normal timetable");
    }

    @Test
    void aWeeklyPlanClassGetsNoNormalTimetable() {
        sessionType("WEEKLY_PLAN");
        assertThat(types.typeOfSession("S1", "CS1")).isEqualTo(Type.WEEKLY_PLAN);
        assertThatCode(() -> types.requireWeeklyPlan("S1", "CS1")).doesNotThrowAnyException();
        assertThatThrownBy(() -> types.requirePeriods("S1", "CS1")).isInstanceOf(RuleException.class)
                .hasMessageContaining("weekly plan");
    }

    @Test
    void aLessonIsCheckedAgainstThePeriodsClass() {
        when(jdbc.queryForList(contains("FROM periods"), eq(String.class), eq("P1"), eq("S1")))
                .thenReturn(List.of("CS1"));
        sessionType("WEEKLY_PLAN");
        assertThatThrownBy(() -> types.requirePeriodsForPeriod("S1", "P1")).isInstanceOf(RuleException.class);
    }

    @Test
    void anUnknownSectionCountsAsNormal() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq("CS9"), eq("S1"))).thenReturn(Collections.emptyList());
        assertThat(types.typeOfSession("S1", "CS9")).isEqualTo(Type.PERIODS);
    }

    @Test
    void weeklyPlanEditsAreRefusedForANormalClass() {
        sessionType("PERIODS");
        var plans = new WeeklyPlanService(jdbc, null);
        assertThatThrownBy(() -> plans.save("S1", "CS1", null, List.of())).isInstanceOf(RuleException.class)
                .hasMessageContaining("normal timetable");
        assertThatThrownBy(() -> plans.plan("S1", "CS1", null)).isInstanceOf(RuleException.class);
    }
}
