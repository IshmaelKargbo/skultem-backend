package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.model.LibraryAttendanceCorrection;
import com.moriba.skultem.domain.model.LibrarySession;
import com.moriba.skultem.domain.model.LibrarySessionRecord;
import com.moriba.skultem.domain.model.LibrarySessionRecord.Status;
import com.moriba.skultem.domain.repository.LibraryAttendanceCorrectionRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRecordRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRepository;

// §7: only a finalized register is corrected, and the original mark is kept alongside the new one.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CorrectLibraryAttendanceUseCaseTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 28);
    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");

    @Mock
    private LibrarySessionRepository sessionRepo;
    @Mock
    private LibrarySessionRecordRepository recordRepo;
    @Mock
    private LibraryAttendanceCorrectionRepository correctionRepo;
    @Mock
    private LibraryAttendanceSupport support;
    @Mock
    private LogActivityUseCase logActivityUseCase;
    @InjectMocks
    private CorrectLibraryAttendanceUseCase useCase;

    private LibrarySession session;
    private LibrarySessionRecord record;

    @BeforeEach
    void setUp() {
        var slot = new LibrarySession.Slot("entry-1", DATE, "cs-1", "JSS 2 A", "sub", "Library", "P3",
                LocalTime.of(10, 0), LocalTime.of(10, 40), null, "term-1", DATE.plusDays(40));
        session = LibrarySession.open("school", slot, "lib", "Librarian", DATE, NOW);
        record = LibrarySessionRecord.onRoster("school", session.getId(), "student-1", "Aminata", "JSS 2 A", false);
        record.mark(Status.ABSENT, null, "lib", NOW);

        when(sessionRepo.findByIdAndSchoolForUpdate(session.getId(), "school")).thenReturn(Optional.of(session));
        when(recordRepo.findByIdAndSchool(record.getId(), "school")).thenReturn(Optional.of(record));
        when(support.now()).thenReturn(NOW);
        when(support.userName("principal")).thenReturn("The Principal");
    }

    @Test
    void aRegisterThatIsStillInProgressIsNotCorrected() {
        assertThatThrownBy(() -> useCase.execute("school", session.getId(), record.getId(), Status.PRESENT, null,
                "Was in the library", "principal"))
                .isInstanceOf(RuleException.class)
                .hasMessageContaining("finalized");
        verify(correctionRepo, never()).save(any());
    }

    @Test
    void aCorrectionKeepsTheOriginalStatusWhoWhenAndWhy() {
        session.finalizeWith(List.of(record), "lib", "Librarian", DATE, NOW);

        useCase.execute("school", session.getId(), record.getId(), Status.PRESENT, null, "Was in the library",
                "principal");

        var saved = ArgumentCaptor.forClass(LibraryAttendanceCorrection.class);
        verify(correctionRepo).save(saved.capture());
        assertThat(saved.getValue().getOriginalStatus()).isEqualTo(Status.ABSENT);
        assertThat(saved.getValue().getCorrectedStatus()).isEqualTo(Status.PRESENT);
        assertThat(saved.getValue().getCorrectedByName()).isEqualTo("The Principal");
        assertThat(saved.getValue().getReason()).isEqualTo("Was in the library");
        assertThat(record.getStatus()).isEqualTo(Status.PRESENT);
        assertThat(record.isCorrected()).isTrue();
    }

    @Test
    void aCorrectionNeedsAReason() {
        session.finalizeWith(List.of(record), "lib", "Librarian", DATE, NOW);

        assertThatThrownBy(() -> useCase.execute("school", session.getId(), record.getId(), Status.PRESENT, null,
                " ", "principal")).isInstanceOf(RuleException.class);
        assertThat(record.getStatus()).isEqualTo(Status.ABSENT);
    }

    @Test
    void aSessionFromAnotherSchoolIsNotFound() {
        when(sessionRepo.findByIdAndSchoolForUpdate(session.getId(), "other")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("other", session.getId(), record.getId(), Status.PRESENT, null,
                "Reason", "principal")).isInstanceOf(NotFoundException.class);
    }
}
