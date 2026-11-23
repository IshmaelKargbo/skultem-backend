package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.repository.ParentPurgeRepository.AccountOutcome;
import com.moriba.skultem.domain.repository.TeacherPurgeRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;

class DeleteTeacherPermanentlyUseCaseTest {

    private final TeacherRepository teacherRepo = mock(TeacherRepository.class);
    private final TeacherPurgeRepository purgeRepo = mock(TeacherPurgeRepository.class);
    private final LogActivityUseCase log = mock(LogActivityUseCase.class);
    private final DeleteTeacherPermanentlyUseCase useCase = new DeleteTeacherPermanentlyUseCase(teacherRepo, purgeRepo, log);

    private void teacher() {
        var user = mock(com.moriba.skultem.domain.model.User.class);
        when(user.getGivenNames()).thenReturn("Fatmata");
        when(user.getFamilyName()).thenReturn("Sesay");
        var t = mock(Teacher.class);
        when(t.getUser()).thenReturn(user);
        when(teacherRepo.findByIdAndSchoolId("t1", "s")).thenReturn(Optional.of(t));
    }

    @Test
    void deletesAStaffMemberWithNoHistory() {
        teacher();
        when(purgeRepo.history("t1")).thenReturn(Map.of());
        when(purgeRepo.purge("s", "t1")).thenReturn(AccountOutcome.DELETED);
        var res = useCase.execute("s", "t1", "  fatmata   SESAY ");
        assertThat(res.name()).isEqualTo("Fatmata Sesay");
        assertThat(res.account()).isEqualTo("DELETED");
    }

    @Test
    void needsTheFullNameTyped() {
        teacher();
        assertThatThrownBy(() -> useCase.execute("s", "t1", "Fatmata")).isInstanceOf(RuleException.class)
                .hasMessageContaining("Fatmata Sesay");
        verify(purgeRepo, never()).purge(any(), any());
    }

    @Test
    void refusesWhenTheyHaveHistoryAndSuggestsDeactivating() {
        teacher();
        when(purgeRepo.history("t1")).thenReturn(Map.of("payslip(s)", 3L));
        assertThatThrownBy(() -> useCase.execute("s", "t1", "Fatmata Sesay")).isInstanceOf(RuleException.class)
                .hasMessageContaining("3 payslip(s)").hasMessageContaining("Deactivate");
        verify(purgeRepo, never()).purge(any(), any());
    }
}
