package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SkullyReminderServiceTest {

    private final SkullyReminderService service = new SkullyReminderService(null, null, null, null, null);

    SkullyReminderServiceTest() {
        ReflectionTestUtils.setField(service, "countryCode", "232");
    }

    @Test
    void savedPhonesBecomeWhatsAppNumbers() {
        assertThat(service.whatsappNumber("077 123 456")).isEqualTo("23277123456");
        assertThat(service.whatsappNumber("+232 76 854 598")).isEqualTo("23276854598");
        assertThat(service.whatsappNumber("23276854598")).isEqualTo("23276854598");
        assertThat(service.whatsappNumber("76854598")).isEqualTo("23276854598");
        assertThat(service.whatsappNumber("+44 7700 900123")).isEqualTo("447700900123");
        assertThat(service.whatsappNumber("123")).isNull();
    }

    @Test
    void aStopMatchesTheNumberHoweverItWasSaved() {
        String stop = SkullyReminderService.optOutKey("23276854598");
        assertThat(SkullyReminderService.optOutKey(service.whatsappNumber("076 854 598"))).isEqualTo(stop);
        assertThat(SkullyReminderService.optOutKey(service.whatsappNumber("+232 76 854 598"))).isEqualTo(stop);
        assertThat(SkullyReminderService.optOutKey(service.whatsappNumber("0023276854598"))).isEqualTo(stop);
        assertThat(SkullyReminderService.optOutKey(service.whatsappNumber("077 123 456"))).isNotEqualTo(stop);
    }

    @Test
    void theWeeklySummaryGoesOutFridayAfternoonOrSaturday() {
        var zone = java.time.ZoneId.of("Africa/Freetown");
        assertThat(SkullyReminderService.weeklyTime(java.time.ZonedDateTime.of(2026, 11, 13, 13, 0, 0, 0, zone))).isFalse();
        assertThat(SkullyReminderService.weeklyTime(java.time.ZonedDateTime.of(2026, 11, 13, 14, 0, 0, 0, zone))).isTrue();
        assertThat(SkullyReminderService.weeklyTime(java.time.ZonedDateTime.of(2026, 11, 14, 9, 0, 0, 0, zone))).isTrue();
        assertThat(SkullyReminderService.weeklyTime(java.time.ZonedDateTime.of(2026, 11, 15, 9, 0, 0, 0, zone))).isFalse();
    }

    @Test
    void theWeekIsMondayToFriday() {
        assertThat(SkullyReminderService.weekLabel(java.time.LocalDate.of(2026, 11, 9))).isEqualTo("9 - 13 Nov 2026");
        assertThat(SkullyReminderService.weekLabel(java.time.LocalDate.of(2026, 11, 30))).isEqualTo("30 Nov - 4 Dec 2026");
    }

    @Test
    void aChildsWeekBecomesFactsAndAPlainLine() {
        var week = new SkullyReminderService.Week();
        week.days = 5;
        week.present = 4;
        week.absentOn.add("Wednesday");
        week.mark("Maths", 90);
        week.mark("English", 80);
        week.note("POSITIVE", "Helped a classmate");
        week.note("NEGATIVE", "Late homework");

        assertThat(week.plain("Fatu", "JSS 1 A")).isEqualTo("Fatu (JSS 1 A): present 4 of 5 days, average 85% in 2 subjects, "
                + "best 90% (Maths), teacher's note: Helped a classmate; To talk about: Late homework.");
        var facts = week.facts("Fatu", "JSS 1 A");
        assertThat(facts.get("averageMark")).isEqualTo(85L);
        assertThat(facts.get("attendance")).isEqualTo(java.util.Map.of("schoolDays", 5L, "present", 4L, "absentOn", List.of("Wednesday")));
        assertThat((List<?>) facts.get("marks")).hasSize(2);
        assertThat((List<?>) facts.get("teacherNotes")).hasSize(2);
    }

    @Test
    void aChildWithOnlyAttendanceGetsAShortLine() {
        var week = new SkullyReminderService.Week();
        week.days = 5;
        week.present = 5;
        assertThat(week.plain("Musa", "Class 3")).isEqualTo("Musa (Class 3): present 5 of 5 days.");
        assertThat(week.facts("Musa", "Class 3").get("averageMark")).isNull();
    }

    @Test
    void thePlainFamilySummaryFitsTheTemplate() {
        assertThat(SkullyReminderService.plainSummary(List.of("Fatu: a.", "Musa: b."))).isEqualTo("Fatu: a. Musa: b.");
        String longOne = SkullyReminderService.plainSummary(List.of("x".repeat(500), "y".repeat(500)));
        assertThat(longOne).hasSize(SkullyReminderService.MAX_SUMMARY).endsWith("...");
    }

    @Test
    void childrenAreNamedTogether() {
        assertThat(SkullyReminderService.names(List.of())).isEqualTo("your child");
        assertThat(SkullyReminderService.names(List.of("Fatu"))).isEqualTo("Fatu");
        assertThat(SkullyReminderService.names(List.of("Fatu", "Musa"))).isEqualTo("Fatu and Musa");
        assertThat(SkullyReminderService.names(List.of("Fatu", "Musa", "Ali"))).isEqualTo("Fatu, Musa and Ali");
    }

    @Test
    void oneParentsRemindersBecomeOneLine() {
        assertThat(SkullyReminderService.join(List.of("Fatu's exams start tomorrow", "Sports day is\ntomorrow")))
                .isEqualTo("Fatu's exams start tomorrow; Sports day is tomorrow");
    }

    @Test
    void aLongListIsCut() {
        var many = java.util.stream.IntStream.range(0, 40).mapToObj(i -> "Reminder number " + i + " ".repeat(5) + "x".repeat(20)).toList();
        String text = SkullyReminderService.join(many);
        assertThat(text.length()).isLessThan(960);
        assertThat(text).endsWith("more - reply to ask");
    }
}
