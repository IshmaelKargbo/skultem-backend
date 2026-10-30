package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.model.LibrarySessionRecord.Status;

class LibrarySessionRecordTest {

    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");

    private LibrarySessionRecord record() {
        return LibrarySessionRecord.onRoster("school-1", "session-1", "student-1", "Aminata Kamara", "JSS 2 A", false);
    }

    @Test
    void everyoneStartsUnmarked() {
        assertThat(record().getStatus()).isEqualTo(Status.UNMARKED);
    }

    @Test
    void unmarkedIsNotSomethingYouCanMarkSomeoneAs() {
        assertThatThrownBy(() -> record().mark(Status.UNMARKED, null, "user-1", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anExcuseReasonIsOptionalAndOnlyKeptForExcused() {
        var r = record();
        r.mark(Status.EXCUSED, "Sick bay", "user-1", NOW);
        assertThat(r.getExcuseReason()).isEqualTo("Sick bay");

        r.mark(Status.PRESENT, "ignored", "user-1", NOW);
        assertThat(r.getExcuseReason()).isNull();

        r.mark(Status.EXCUSED, null, "user-1", NOW);
        assertThat(r.getStatus()).isEqualTo(Status.EXCUSED);
    }

    @Test
    void aStudentTakenOffTheRosterCantBeMarkedUntilAddedBack() {
        var r = record();
        r.mark(Status.PRESENT, null, "user-1", NOW);
        r.removeFromRoster();

        assertThat(r.isCounted()).isFalse();
        assertThatThrownBy(() -> r.mark(Status.PRESENT, null, "user-1", NOW))
                .isInstanceOf(IllegalStateException.class);

        r.restoreToRoster();
        assertThat(r.isCounted()).isTrue();
        assertThat(r.getStatus()).isEqualTo(Status.UNMARKED);
    }

    @Test
    void aCorrectionReturnsTheOriginalStatusAndFlagsTheRow() {
        var r = record();
        r.mark(Status.ABSENT, null, "user-1", NOW);

        var original = r.correct(Status.PRESENT, null, "user-2", NOW);

        assertThat(original).isEqualTo(Status.ABSENT);
        assertThat(r.getStatus()).isEqualTo(Status.PRESENT);
        assertThat(r.isCorrected()).isTrue();
    }

    @Test
    void aCorrectionMustChangeTheStatusAndCantGoBackToUnmarked() {
        var r = record();
        r.mark(Status.ABSENT, null, "user-1", NOW);

        assertThatThrownBy(() -> r.correct(Status.ABSENT, null, "user-2", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> r.correct(Status.UNMARKED, null, "user-2", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
