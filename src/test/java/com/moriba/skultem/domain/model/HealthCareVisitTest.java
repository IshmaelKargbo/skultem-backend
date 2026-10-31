package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.model.HealthCareVisit.Closing;
import com.moriba.skultem.domain.model.HealthCareVisit.Complaint;
import com.moriba.skultem.domain.model.HealthCareVisit.Outcome;

// docs/HEALTH_CARE.md §5: starting, closing, following up, notes and voiding.
class HealthCareVisitTest {

    private static final Instant ARRIVED = Instant.parse("2026-10-28T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-28T10:30:00Z");
    private static final Instant DAY_END = Instant.parse("2026-10-28T15:00:00Z");
    private static final List<Complaint> HEADACHE = List.of(new Complaint("HEADACHE", "Headache", false));

    private static HealthCareVisit visit() {
        return HealthCareVisit.start("school", "s1", "Aminata Kamara", "JSS 2 A", ARRIVED, DAY_END, HEADACHE, null,
                null, null, null, null, null, "nurse", "Nurse", NOW);
    }

    private static Closing returned(String treatment) {
        return new Closing(Outcome.RETURNED_TO_CLASS, null, treatment, null, null, null, null, null, null);
    }

    @Test
    void aVisitIsForTodayOnlyAndNotInTheFuture() {
        assertThatThrownBy(() -> HealthCareVisit.start("school", "s1", "A", null, Instant.parse("2026-10-27T10:00:00Z"),
                DAY_END, HEADACHE, null, null, null, null, null, null, "n", "N", NOW))
                .hasMessageContaining("today");
        assertThatThrownBy(() -> HealthCareVisit.start("school", "s1", "A", null, NOW.plusSeconds(3600), DAY_END,
                HEADACHE, null, null, null, null, null, null, "n", "N", NOW))
                .hasMessageContaining("future");
    }

    @Test
    void otherNeedsADescriptionAndTemperatureMustBeSensible() {
        var other = List.of(new Complaint("OTHER", "Other", false));
        assertThatThrownBy(() -> HealthCareVisit.start("school", "s1", "A", null, ARRIVED, DAY_END, other, " ", null,
                null, null, null, null, "n", "N", NOW)).hasMessageContaining("Other");
        assertThatThrownBy(() -> HealthCareVisit.start("school", "s1", "A", null, ARRIVED, DAY_END, HEADACHE, null,
                null, new BigDecimal("50.0"), null, null, null, "n", "N", NOW)).hasMessageContaining("Temperature");
    }

    @Test
    void closingNeedsTheTreatmentWhenTheSchoolRequiresIt() {
        var v = visit();
        assertThatThrownBy(() -> v.close(returned(null), true, false, "n", "N", NOW)).hasMessageContaining("treatment");
        v.close(returned("Rested, water"), true, false, "n", "N", NOW);
        assertThat(v.isClosed()).isTrue();
        assertThat(v.getLeftAt()).isEqualTo(NOW);
        assertThat(v.minutesIn(NOW)).isEqualTo(30);
    }

    @Test
    void sentHomeNeedsWhoCollectedAndReferredNeedsWhere() {
        var v = visit();
        assertThatThrownBy(() -> v.close(new Closing(Outcome.SENT_HOME, null, "x", "Mrs Kamara", null, null, null,
                null, null), false, false, "n", "N", NOW)).hasMessageContaining("collected");
        assertThatThrownBy(() -> v.close(new Closing(Outcome.REFERRED, null, "x", null, null, " ", true, null, null),
                false, false, "n", "N", NOW)).hasMessageContaining("referred");
        v.close(new Closing(Outcome.REFERRED, null, "x", "ignored", "ignored", "Connaught Hospital", true, "Nurse",
                null), false, false, "n", "N", NOW);
        assertThat(v.getReferredTo()).isEqualTo("Connaught Hospital");
        assertThat(v.getCollectedBy()).isNull();
    }

    @Test
    void aClosedVisitIsNotEditedOnlyGivenNotes() {
        var v = visit();
        v.close(returned("Rest"), true, false, "n", "N", NOW);
        assertThatThrownBy(() -> v.update(HEADACHE, null, null, null, "changed", null, NOW))
                .hasMessageContaining("closed");
        assertThatThrownBy(() -> v.addMedication("Paracetamol", "500mg", null, null, "n", "N", NOW))
                .hasMessageContaining("closed");
        v.addNote("Parent called back", "p", "Principal", NOW);
        assertThat(v.getFollowUpNotes()).hasSize(1);
    }

    @Test
    void afterTheSchoolDayAVisitIsFollowedUpWithAReasonAndNoDepartureTime() {
        var v = visit();
        Instant evening = Instant.parse("2026-10-28T17:00:00Z");
        assertThat(v.needsFollowUp(evening)).isTrue();
        assertThatThrownBy(() -> v.close(returned("Rest"), false, false, "n", "N", evening))
                .hasMessageContaining("Follow it up");
        assertThatThrownBy(() -> v.followUp(returned(null), " ", "n", "N", evening)).hasMessageContaining("reason");
        v.followUp(returned(null), "Went home with sibling, not recorded", "n", "N", evening);
        assertThat(v.isClosed()).isTrue();
        assertThat(v.getLeftAt()).isNull();
        assertThat(v.minutesIn(evening)).isNull();
    }

    @Test
    void voidedVisitsDoNotCountAndContinuationsDoNotCount() {
        var v = visit();
        assertThat(v.countsTowardsLimits()).isTrue();
        v.voidVisit("Wrong student", "n", "N", NOW);
        assertThat(v.countsTowardsLimits()).isFalse();
        assertThatThrownBy(() -> v.voidVisit("again", "n", "N", NOW)).hasMessageContaining("already");

        var continuation = HealthCareVisit.start("school", "s1", "A", null, ARRIVED, DAY_END, HEADACHE, null, null,
                null, null, "earlier-visit", null, "n", "N", NOW);
        assertThat(continuation.countsTowardsLimits()).isFalse();
    }

    @Test
    void parentsCanBeNotifiedByHandOnceButRetriedAfterReachingNobody() {
        var v = visit();
        v.close(returned("Rest"), false, false, "n", "N", NOW);
        v.requireManualNotifyAllowed();
        v.recordNotified(List.of(), true, "NONE", NOW);
        v.requireManualNotifyAllowed(); // nobody was reached: a retry is allowed
        v.recordNotified(List.of(new HealthCareVisit.ParentNotified("u1", "Parent", NOW, true)), true, null, NOW);
        assertThatThrownBy(v::requireManualNotifyAllowed).hasMessageContaining("already");
    }
}
