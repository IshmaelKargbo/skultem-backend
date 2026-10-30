package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.model.LibrarySessionRecord.Status;

// Workflow B lifecycle: open on or after the day, finalize only with every student marked, cancel with a reason,
// and nothing changes once the term is over.
class LibrarySessionTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 28);
    private static final LocalDate TERM_END = LocalDate.of(2026, 12, 18);
    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");

    private final LibrarySession.Slot slot = new LibrarySession.Slot("entry-1", DATE, "cs-1", "JSS 2 A", "sub-lib",
            "Library", "Period 3", LocalTime.of(10, 0), LocalTime.of(10, 40), "Library Room", "term-1", TERM_END);

    private LibrarySession open() {
        return LibrarySession.open("school-1", slot, "user-1", "Librarian", DATE.plusDays(1), NOW);
    }

    private LibrarySessionRecord record(String student, Status status) {
        var r = LibrarySessionRecord.onRoster("school-1", "session-1", student, student, "JSS 2 A", false);
        if (status != Status.UNMARKED) {
            r.mark(status, null, "user-1", NOW);
        }
        return r;
    }

    @Test
    void aSessionCantBeOpenedBeforeItsDay() {
        assertThatThrownBy(() -> LibrarySession.open("school-1", slot, "user-1", "Librarian", DATE.minusDays(1), NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aSessionCantBeOpenedAfterItsTermHasEnded() {
        assertThatThrownBy(() -> LibrarySession.open("school-1", slot, "user-1", "Librarian", TERM_END.plusDays(1),
                NOW)).isInstanceOf(IllegalStateException.class).hasMessageContaining("term has ended");
    }

    @Test
    void openingCopiesTheTimetableDetailsOntoTheSession() {
        var session = open();

        assertThat(session.getStatus()).isEqualTo(LibrarySession.Status.IN_PROGRESS);
        assertThat(session.getClassName()).isEqualTo("JSS 2 A");
        assertThat(session.getPeriodName()).isEqualTo("Period 3");
        assertThat(session.getRoomName()).isEqualTo("Library Room");
        assertThat(session.getTimetableEntryId()).isEqualTo("entry-1");
    }

    @Test
    void itCantBeFinalizedWhileAnyoneIsUnmarked() {
        var session = open();
        var records = List.of(record("a", Status.PRESENT), record("b", Status.UNMARKED), record("c", Status.UNMARKED));

        assertThatThrownBy(() -> session.finalizeWith(records, "user-1", "Librarian", DATE, NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2 students are still unmarked");
        assertThat(session.isInProgress()).isTrue();
    }

    @Test
    void anEmptyRegisterCantBeFinalized() {
        var session = open();

        assertThatThrownBy(() -> session.finalizeWith(List.of(), "user-1", "Librarian", DATE, NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aStudentTakenOffTheRosterDoesntBlockFinalizing() {
        var session = open();
        var removed = record("b", Status.UNMARKED);
        removed.removeFromRoster();

        session.finalizeWith(List.of(record("a", Status.ABSENT), removed), "user-1", "Librarian", DATE, NOW);

        assertThat(session.isCompleted()).isTrue();
        assertThat(session.getFinalizedByName()).isEqualTo("Librarian");
    }

    @Test
    void aFinalizedSessionCantBeEditedOrCancelled() {
        var session = open();
        session.finalizeWith(List.of(record("a", Status.PRESENT)), "user-1", "Librarian", DATE, NOW);

        assertThatThrownBy(() -> session.requireEditable(DATE)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("finalized");
        assertThatThrownBy(() -> session.cancel("user-2", "Principal", "Clash", DATE, NOW))
                .isInstanceOf(IllegalStateException.class);
        session.requireCorrectable();
    }

    @Test
    void cancellingNeedsAReasonAndCantBeUndone() {
        var session = open();

        assertThatThrownBy(() -> session.cancel("user-1", "Librarian", " ", DATE, NOW))
                .isInstanceOf(IllegalArgumentException.class);

        session.cancel("user-1", "Librarian", "Exams in the library", DATE, NOW);

        assertThat(session.isCancelled()).isTrue();
        assertThatThrownBy(() -> session.requireEditable(DATE)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(session::requireCorrectable).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anUpcomingSessionCanBeCancelledWithoutBeingOpened() {
        var session = LibrarySession.cancelBeforeOpening("school-1", slot, "user-2", "Principal", "Sports day",
                DATE.minusDays(3), NOW);

        assertThat(session.isCancelled()).isTrue();
        assertThat(session.getOpenedAt()).isNull();
        assertThat(session.getCancelReason()).isEqualTo("Sports day");
    }

    @Test
    void itIsIncompleteOnceThePeriodEndsUnfinalized() {
        var session = open();

        assertThat(session.isIncomplete(LocalDateTime.of(DATE, LocalTime.of(10, 30)))).isFalse();
        assertThat(session.isIncomplete(LocalDateTime.of(DATE, LocalTime.of(10, 41)))).isTrue();
    }

    @Test
    void nothingChangesOnceTheTermIsOver() {
        var session = open();

        assertThatThrownBy(() -> session.requireEditable(TERM_END.plusDays(1)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("term has ended");
    }
}
