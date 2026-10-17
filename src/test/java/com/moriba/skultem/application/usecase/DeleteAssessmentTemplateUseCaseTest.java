package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.AssessmentTemplate;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;

class DeleteAssessmentTemplateUseCaseTest {

    private final AssessmentTemplateRepository templateRepo = mock(AssessmentTemplateRepository.class);
    private final AssessmentRepository assessmentRepo = mock(AssessmentRepository.class);
    private final ClassRepository classRepo = mock(ClassRepository.class);
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo = mock(ClassSubjectAssessmentLifeCycleRepository.class);
    private final DeleteAssessmentTemplateUseCase useCase = new DeleteAssessmentTemplateUseCase(templateRepo,
            assessmentRepo, classRepo, cycleRepo);

    private void templateExists() {
        when(templateRepo.findByIdAndSchoolId("t", "s"))
                .thenReturn(Optional.of(AssessmentTemplate.create("t", "s", "JSS", "d", 50)));
    }

    @Test
    void anUnusedTemplateIsDeletedWithItsAssessments() {
        templateExists();

        assertThat(useCase.execute("s", "t")).isEqualTo("JSS");

        verify(assessmentRepo).deleteAllByTemplateIdAndSchoolId("t", "s");
        verify(templateRepo).deleteByIdAndSchoolId("t", "s");
    }

    @Test
    void aTemplateUsedByClassesIsRefused() {
        templateExists();
        when(classRepo.countByTemplate("t")).thenReturn(2L);

        assertThatThrownBy(() -> useCase.execute("s", "t")).isInstanceOf(RuleException.class)
                .hasMessageContaining("used by 2 classes");
        verify(templateRepo, never()).deleteByIdAndSchoolId("t", "s");
    }

    @Test
    void aTemplateWithRecordedAssessmentsIsRefused() {
        templateExists();
        when(cycleRepo.countByTemplate("t")).thenReturn(5L);

        assertThatThrownBy(() -> useCase.execute("s", "t")).isInstanceOf(RuleException.class)
                .hasMessageContaining("already been used");
        verify(templateRepo, never()).deleteByIdAndSchoolId("t", "s");
    }
}
