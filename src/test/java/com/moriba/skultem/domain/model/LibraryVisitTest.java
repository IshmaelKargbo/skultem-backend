package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

// Workflow A: a visit is closed by a real check-out or, once the school day is over, by a follow-up that never
// invents a check-out time.
class LibraryVisitTest {

    private static final Instant IN = Instant.parse("2026-10-29T09:00:00Z");
    private static final Instant DAY_ENDS = Instant.parse("2026-10-29T14:30:00Z");

    private LibraryVisit visit() {
        return LibraryVisit.checkIn("school-1", "student-1", "Aminata Kamara", "JSS 2 A", "user-1", "Librarian",
                IN, DAY_ENDS);
    }

    @Test
    void checkingOutGivesADuration() {
        var visit = visit();

        visit.checkOut("user-1", "Librarian", IN.plusSeconds(45 * 60));

        assertThat(visit.getStatus()).isEqualTo(LibraryVisit.Status.CHECKED_OUT);
        assertThat(visit.durationMinutes()).isEqualTo(45);
    }

    @Test
    void aVisitCantBeCheckedOutTwice() {
        var visit = visit();
        visit.checkOut("user-1", "Librarian", IN.plusSeconds(60));

        assertThatThrownBy(() -> visit.checkOut("user-1", "Librarian", IN.plusSeconds(120)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aVisitIsOnlyUnresolvedOnceTheSchoolDayHasEnded() {
        var visit = visit();

        assertThat(visit.isUnresolved(DAY_ENDS.minusSeconds(1))).isFalse();
        assertThat(visit.isUnresolved(DAY_ENDS)).isTrue();
    }

    @Test
    void itCantBeFollowedUpWhileTheSchoolDayIsStillOn() {
        var visit = visit();

        assertThatThrownBy(() -> visit.resolve("user-2", "Principal", "Left early", DAY_ENDS.minusSeconds(60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("check the student out");
    }

    @Test
    void followingUpNeedsAReason() {
        var visit = visit();

        assertThatThrownBy(() -> visit.resolve("user-2", "Principal", "  ", DAY_ENDS.plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void followingUpClosesItWithoutMakingUpACheckOutTime() {
        var visit = visit();

        visit.resolve("user-2", "Principal", "Student went home at lunch", DAY_ENDS.plusSeconds(60));

        assertThat(visit.getStatus()).isEqualTo(LibraryVisit.Status.RESOLVED);
        assertThat(visit.getCheckedOutAt()).isNull();
        assertThat(visit.durationMinutes()).isNull();
        assertThat(visit.getCheckedInAt()).isEqualTo(IN);
        assertThat(visit.getResolvedByName()).isEqualTo("Principal");
        assertThat(visit.getResolutionReason()).isEqualTo("Student went home at lunch");
    }
}
