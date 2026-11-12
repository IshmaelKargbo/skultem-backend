package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SkullyReminderServiceTest {

    private final SkullyReminderService service = new SkullyReminderService(null, null, null, null);

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
