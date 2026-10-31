package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.moriba.skultem.domain.model.HealthCareVisit;
import com.moriba.skultem.domain.model.HealthCareVisit.Closing;
import com.moriba.skultem.domain.model.HealthCareVisit.Complaint;
import com.moriba.skultem.domain.model.HealthCareVisit.Outcome;
import com.moriba.skultem.domain.repository.HealthCareTurnedAwayRepository;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;

// docs/HEALTH_CARE.md §10: the visits summary - voided visits left out, per class and per day.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HealthCareSummaryTest {

    @Mock
    private HealthCareVisitRepository visitRepo;
    @Mock
    private HealthCareTurnedAwayRepository turnedAwayRepo;
    @InjectMocks
    private HealthCareService service;

    private static HealthCareVisit visit(String student, String cls, String arrived, Outcome outcome, int minutes,
            boolean urgent) {
        Instant at = Instant.parse(arrived);
        var v = HealthCareVisit.start("school", student, student, cls, at, at.plusSeconds(8 * 3600),
                List.of(new Complaint(urgent ? "BREATHING" : "HEADACHE", urgent ? "Breathing difficulty" : "Headache",
                        urgent)), null, null, null, null, null, null, "n", "Nurse", at);
        if (outcome != null) {
            Instant left = at.plusSeconds(minutes * 60L);
            v.close(new Closing(outcome, left, "Rest", "Mum", "Mother", "Clinic", false, null, null), false, false,
                    "n", "Nurse", left);
        }
        return v;
    }

    @Test
    void summarisesByClassAndDayWithoutVoidedVisits() {
        var voided = visit("s4", "JSS 1", "2026-10-27T09:00:00Z", Outcome.RETURNED_TO_CLASS, 10, false);
        voided.voidVisit("Wrong student", "n", "Nurse", Instant.parse("2026-10-27T10:00:00Z"));
        when(visitRepo.findBetween(eq("school"), any(), any())).thenReturn(List.of(
                visit("s1", "JSS 1", "2026-10-26T09:00:00Z", Outcome.RETURNED_TO_CLASS, 20, false),
                visit("s1", "JSS 1", "2026-10-28T09:00:00Z", Outcome.SENT_HOME, 40, false),
                visit("s2", "JSS 2", "2026-10-28T10:00:00Z", Outcome.REFERRED, 30, true),
                visit("s3", "JSS 2", "2026-10-28T11:00:00Z", null, 0, false),
                voided));
        when(turnedAwayRepo.findBetween(eq("school"), any(), any())).thenReturn(List.of());

        var s = service.summary("school", LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 28));

        assertThat(s.visits()).isEqualTo(4);
        assertThat(s.students()).isEqualTo(3);
        assertThat(s.urgent()).isEqualTo(1);
        assertThat(s.stillOpen()).isEqualTo(1);
        assertThat(s.averageMinutes()).isEqualTo(30); // (20 + 40 + 30) / 3 closed visits
        assertThat(s.days()).hasSize(3);
        assertThat(s.days().get(1).visits()).isZero(); // the 27th: only the voided visit
        assertThat(s.days().get(2).visits()).isEqualTo(3);

        var jss1 = s.classes().stream().filter(c -> c.className().equals("JSS 1")).findFirst().orElseThrow();
        assertThat(jss1.visits()).isEqualTo(2);
        assertThat(jss1.students()).isEqualTo(1);
        assertThat(jss1.sentHome()).isEqualTo(1);
        var jss2 = s.classes().stream().filter(c -> c.className().equals("JSS 2")).findFirst().orElseThrow();
        assertThat(jss2.referred()).isEqualTo(1);
        assertThat(jss2.stillOpen()).isEqualTo(1);
    }
}
