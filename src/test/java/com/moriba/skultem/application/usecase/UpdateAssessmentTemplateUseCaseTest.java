package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.usecase.UpdateAssessmentTemplateUseCase.Details;
import com.moriba.skultem.application.usecase.UpdateAssessmentTemplateUseCase.Item;
import com.moriba.skultem.domain.model.Assessment;
import com.moriba.skultem.domain.model.AssessmentTemplate;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.TemplateMaintenanceRepository;
import com.moriba.skultem.domain.repository.TemplateMaintenanceRepository.Usage;

class UpdateAssessmentTemplateUseCaseTest {

    private final AssessmentTemplateRepository templateRepo = mock(AssessmentTemplateRepository.class);
    private final AssessmentRepository assessmentRepo = mock(AssessmentRepository.class);
    private final AssessmentScoreRepository scoreRepo = mock(AssessmentScoreRepository.class);
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo = mock(ClassSubjectAssessmentLifeCycleRepository.class);
    private final StudentAssessmentRepository studentAssessmentRepo = mock(StudentAssessmentRepository.class);
    private final TemplateMaintenanceRepository maintenance = mock(TemplateMaintenanceRepository.class);
    private final UpdateAssessmentTemplateUseCase useCase = new UpdateAssessmentTemplateUseCase(templateRepo,
            assessmentRepo, scoreRepo, cycleRepo, studentAssessmentRepo, maintenance);

    private AssessmentTemplate template;
    private Assessment test1;
    private Assessment exam;

    @BeforeEach
    void setUp() {
        template = AssessmentTemplate.create("t", "s", "JSS", "d", 50);
        test1 = Assessment.create("a1", "s", template, "Test 1", 40, 1);
        exam = Assessment.create("a2", "s", template, "Exam", 60, 2);
        when(templateRepo.findByIdAndSchoolId("t", "s")).thenReturn(Optional.of(template));
        when(assessmentRepo.findAllByTemplateIdAndSchoolId("t", "s")).thenReturn(List.of(test1, exam));
        when(maintenance.usage("s", "t")).thenReturn(new Usage(List.of("JSS 1"), 0, 0));
        when(cycleRepo.findAllByTemplate("t")).thenReturn(List.of());
    }

    @Test
    void reweightingKeepsTheTestsAndUpdatesTheScoresWeights() {
        useCase.execute("s", "t", new Details("JSS 2026", 45, "new"),
                List.of(new Item("a1", "Test 1", 50), new Item("a2", "Exam", 50)), false);

        verify(maintenance).updateScoreWeight("a1", 50);
        verify(maintenance).updateScoreWeight("a2", 50);
        verify(maintenance, never()).deleteAssessmentCascade(any());
        assertThat(template.getName()).isEqualTo("JSS 2026");
        assertThat(template.getPassMark()).isEqualTo(45);
        assertThat(test1.getWeight()).isEqualTo(50);
    }

    @Test
    void aTestMatchedByNameKeepsItsIdentityWhenNoIdIsSent() {
        useCase.execute("s", "t", null, List.of(new Item(null, "test 1", 40), new Item(null, "Exam", 60)), false);

        verify(maintenance, never()).deleteAssessmentCascade(any());
        verify(maintenance, never()).updateScoreWeight(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void submittedTestsBlockTheChangeUnlessForced() {
        when(maintenance.usage("s", "t")).thenReturn(new Usage(List.of("Class 4", "Class 5"), 6, 30));

        assertThatThrownBy(() -> useCase.execute("s", "t", null,
                List.of(new Item("a1", "Test 1", 50), new Item("a2", "Exam", 50)), false))
                .isInstanceOf(RuleException.class).hasMessageContaining("6 tests have already been submitted");
        verify(maintenance, never()).updateScoreWeight(any(), org.mockito.ArgumentMatchers.anyInt());

        useCase.execute("s", "t", null, List.of(new Item("a1", "Test 1", 50), new Item("a2", "Exam", 50)), true);
        verify(maintenance).updateScoreWeight("a1", 50);
    }

    @Test
    void removingATestWithMarksNeedsConfirmation() {
        when(maintenance.markedScoresOf("a2")).thenReturn(12L);

        assertThatThrownBy(() -> useCase.execute("s", "t", null, List.of(new Item("a1", "Test 1", 100)), false))
                .isInstanceOf(RuleException.class).hasMessageContaining("Exam already has marks");
        verify(maintenance, never()).deleteAssessmentCascade("a2");

        useCase.execute("s", "t", null, List.of(new Item("a1", "Test 1", 100)), true);
        verify(maintenance).deleteAssessmentCascade("a2");
    }

    @Test
    void weightsOverOneHundredAreRefused() {
        assertThatThrownBy(() -> useCase.execute("s", "t", null,
                List.of(new Item("a1", "Test 1", 70), new Item("a2", "Exam", 60)), false))
                .isInstanceOf(RuleException.class).hasMessageContaining("cannot exceed 100");
    }
}
