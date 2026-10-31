package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.Period;
import com.moriba.skultem.domain.model.Timetable;
import com.moriba.skultem.domain.repository.PeriodRepository;
import com.moriba.skultem.domain.repository.TimetableRepository;

// A period is deleted only within the caller's school; its timetable entries go with it.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeletePeriodUseCaseTest {

    @Mock
    private PeriodRepository periodRepo;
    @Mock
    private TimetableRepository timetableRepo;
    @Mock
    private LogActivityUseCase logActivityUseCase;
    @InjectMocks
    private DeletePeriodUseCase useCase;

    private Period period(String schoolId) {
        var period = mock(Period.class);
        when(period.getId()).thenReturn("p1");
        when(period.getSchoolId()).thenReturn(schoolId);
        when(period.getName()).thenReturn("Period 8");
        when(periodRepo.findById("p1")).thenReturn(Optional.of(period));
        return period;
    }

    @Test
    void anotherSchoolsPeriodIsNotFoundAndNothingIsDeleted() {
        period("other-school");

        assertThatThrownBy(() -> useCase.execute("school", "p1")).isInstanceOf(NotFoundException.class);
        verify(timetableRepo, never()).delete(any());
        verify(periodRepo, never()).delete(any());
    }

    @Test
    void ownPeriodIsDeletedWithItsTimetableEntries() {
        var period = period("school");
        var entry = mock(Timetable.class);
        when(timetableRepo.findAllByPeriodId("p1")).thenReturn(List.of(entry));

        useCase.execute("school", "p1");

        verify(timetableRepo).delete(entry);
        verify(periodRepo).delete(period);
    }
}
