package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.Family;

class UpdateStudentFamilyUseCaseTest {

    private final StudentRepository repo = mock(StudentRepository.class);
    private final GetStudentUseCase get = mock(GetStudentUseCase.class);
    private final LogActivityUseCase log = mock(LogActivityUseCase.class);
    private final UpdateStudentFamilyUseCase useCase = new UpdateStudentFamilyUseCase(repo, get, log);

    @Test
    void savesTheTidiedDetails() {
        var student = mock(Student.class);
        when(repo.findByIdAndSchoolId("st1", "s")).thenReturn(Optional.of(student));

        useCase.execute("s", "st1", new Family("  Mohamed   Kamara ", "", "Driver", null, " ", "076 123456"));

        var captor = ArgumentCaptor.forClass(Family.class);
        verify(student).changeFamily(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue())
                .isEqualTo(new Family("Mohamed Kamara", null, "Driver", null, null, "076 123456"));
        verify(repo).save(student);
    }

    @Test
    void needsAtLeastOneParentName() {
        var student = mock(Student.class);
        when(repo.findByIdAndSchoolId("st1", "s")).thenReturn(Optional.of(student));
        assertThatThrownBy(() -> useCase.execute("s", "st1", new Family(" ", null, null, null, "076", null)))
                .isInstanceOf(RuleException.class);
        verify(repo, never()).save(any());
    }
}
