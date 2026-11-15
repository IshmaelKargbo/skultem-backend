package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class WeeklyPlanServiceTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final WeeklyPlanService service = new WeeklyPlanService(jdbc, null);

    @Test
    void schoolDaysAreMondayToFridayWithoutHolidays() {
        // November 2026: 21 weekdays; 27 Nov is a holiday.
        when(jdbc.queryForList(anyString(), eq("S1"), any(), any()))
                .thenReturn(List.of(Map.of("date", Date.valueOf("2026-11-27"), "name", "Founders' Day")));
        List<LocalDate> days = service.schoolDays("S1", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        assertThat(days).hasSize(20).doesNotContain(LocalDate.of(2026, 11, 27));
        assertThat(days).allMatch(d -> d.getDayOfWeek().getValue() <= 5);
        assertThat(days.get(0)).isEqualTo(LocalDate.of(2026, 11, 2));
    }

    @Test
    void weekLabelsSpanMonths() {
        assertThat(WeeklyPlanService.label(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6))).isEqualTo("2 - 6 Nov");
        assertThat(WeeklyPlanService.label(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 4))).isEqualTo("30 Nov - 4 Dec");
    }

    @Test
    void sessionsAreCheckedAndCleaned() {
        var clean = WeeklyPlanService.validate(List.of(new WeeklyPlanService.Slot("  Morning ", "08:30", "10:00"),
                new WeeklyPlanService.Slot("Story time", "", "")));
        assertThat(clean).extracting("name").containsExactly("Morning", "Story time");
        assertThatThrownBy(() -> WeeklyPlanService.validate(List.of())).hasMessageContaining("at least one");
        assertThatThrownBy(() -> WeeklyPlanService.validate(List.of(new WeeklyPlanService.Slot("", "08:00", "09:00"))))
                .hasMessageContaining("needs a name");
        assertThatThrownBy(() -> WeeklyPlanService.validate(List.of(new WeeklyPlanService.Slot("Late", "10:00", "09:00"))))
                .hasMessageContaining("must end after it starts");
        assertThatThrownBy(() -> WeeklyPlanService.validate(java.util.Collections.nCopies(7,
                new WeeklyPlanService.Slot("x", "", "")))).hasMessageContaining("At most 6");
    }
}
