package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.model.LibrarySession;
import com.moriba.skultem.domain.model.LibrarySessionRecord;
import com.moriba.skultem.domain.model.LibrarySessionRecord.Status;
import com.moriba.skultem.domain.model.LibraryVisit;
import com.moriba.skultem.domain.repository.LibrarySessionRecordRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRepository;
import com.moriba.skultem.domain.repository.LibraryVisitRepository;

// Finalizing a session checks out the roster's open visits from that day.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinalizeLibrarySessionUseCaseTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 28);
    // Freetown is UTC, so these are the same local times.
    private static final Instant CHECKED_IN = Instant.parse("2026-10-28T10:05:00Z");
    private static final Instant NOW = Instant.parse("2026-10-28T10:45:00Z");
    private static final Instant DAY_END = Instant.parse("2026-10-28T15:00:00Z");

    @Mock
    private LibrarySessionRepository sessionRepo;
    @Mock
    private LibrarySessionRecordRepository recordRepo;
    @Mock
    private LibraryVisitRepository visitRepo;
    @Mock
    private LibraryAttendanceSupport support;
    @Mock
    private LogActivityUseCase logActivityUseCase;
    @InjectMocks
    private FinalizeLibrarySessionUseCase useCase;

    private LibrarySession session;
    private LibrarySessionRecord record;

    @BeforeEach
    void setUp() {
        var slot = new LibrarySession.Slot("entry-1", DATE, "cs-1", "JSS 2 A", "sub", "Library", "P3",
                LocalTime.of(10, 0), LocalTime.of(10, 40), null, "term-1", DATE.plusDays(40));
        session = LibrarySession.open("school", slot, "lib", "Librarian", DATE, NOW);
        record = LibrarySessionRecord.onRoster("school", session.getId(), "student-1", "Aminata", "JSS 2 A", false);

        when(sessionRepo.findByIdAndSchoolForUpdate(session.getId(), "school")).thenReturn(Optional.of(session));
        when(recordRepo.findBySession(session.getId(), "school")).thenReturn(List.of(record));
        when(support.now()).thenReturn(NOW);
        when(support.today()).thenReturn(DATE);
        when(support.userName("lib")).thenReturn("Librarian");
    }

    private LibraryVisit openVisit(Instant checkedInAt) {
        var visit = LibraryVisit.checkIn("school", "student-1", "Aminata", "JSS 2 A", "lib", "Librarian",
                checkedInAt, DAY_END);
        when(visitRepo.findOpenByStudents(eq("school"), anyCollection())).thenReturn(List.of(visit));
        when(visitRepo.findByIdAndSchoolForUpdate(visit.getId(), "school")).thenReturn(Optional.of(visit));
        return visit;
    }

    @Test
    void finalizingChecksOutTheRostersOpenVisitsFromThatDay() {
        record.mark(Status.PRESENT, null, "lib", NOW);
        var visit = openVisit(CHECKED_IN);

        useCase.execute("school", session.getId(), "lib");

        assertThat(session.isCompleted()).isTrue();
        assertThat(visit.getStatus()).isEqualTo(LibraryVisit.Status.CHECKED_OUT);
        assertThat(visit.getCheckedOutAt()).isEqualTo(NOW);
        assertThat(visit.getCheckedOutBy()).isEqualTo("lib");
        verify(visitRepo).save(visit);
    }

    @Test
    void anOpenVisitFromAnEarlierDayIsLeftToBeResolved() {
        record.mark(Status.PRESENT, null, "lib", NOW);
        var visit = openVisit(CHECKED_IN.minusSeconds(86_400));

        useCase.execute("school", session.getId(), "lib");

        assertThat(visit.isOpen()).isTrue();
        verify(visitRepo, never()).save(any());
    }

    @Test
    void aRefusedFinalizeChecksNobodyOut() {
        var visit = openVisit(CHECKED_IN);

        assertThatThrownBy(() -> useCase.execute("school", session.getId(), "lib"))
                .isInstanceOf(RuleException.class)
                .hasMessageContaining("unmarked");
        assertThat(visit.isOpen()).isTrue();
        verify(visitRepo, never()).save(any());
    }
}
