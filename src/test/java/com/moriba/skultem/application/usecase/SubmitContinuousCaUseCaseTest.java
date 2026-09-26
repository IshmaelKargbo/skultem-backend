package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.AssessmentStructureService;
import com.moriba.skultem.domain.model.AssessmentCaEntry;
import com.moriba.skultem.domain.model.AssessmentConfiguration;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle.Status;
import com.moriba.skultem.domain.model.StudentAssessment;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;

// "6 weeks of CA" means all six are in, for everyone, before the CA is submitted and the formal test opens.
@ExtendWith(MockitoExtension.class)
class SubmitContinuousCaUseCaseTest {

    private static final String SCHOOL = "school-1";

    @Mock
    private TeacherSubjectRepository teacherSubjectRepo;
    @Mock
    private StudentAssessmentRepository studentAssessmentRepo;
    @Mock
    private AssessmentScoreRepository assessmentScoreRepo;
    @Mock
    private AssessmentCaEntryRepository caEntryRepo;
    @Mock
    private ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    @Mock
    private AssessmentStructureService structureService;
    @InjectMocks
    private SubmitContinuousCaUseCase useCase;

    private ClassSubjectAssessmentLifeCycle cycle;
    private AssessmentScore score;

    @BeforeEach
    void setUp() {
        var ts = mock(TeacherSubject.class, RETURNS_DEEP_STUBS);
        when(ts.getSubject().getId()).thenReturn("subject-1");
        when(ts.getSession().getId()).thenReturn("session-1");
        when(teacherSubjectRepo.findByIdAndSchoolId("ts-1", SCHOOL)).thenReturn(Optional.of(ts));

        cycle = ClassSubjectAssessmentLifeCycle.create("cycle-1", SCHOOL, mock(TeacherSubject.class),
                mock(com.moriba.skultem.domain.model.Term.class), mock(com.moriba.skultem.domain.model.Assessment.class),
                Status.DRAFT);
        cycle.freezeStructure(AssessmentConfiguration.create(SCHOOL, "sec", AssessmentStructure.CA_AND_TEST, 30, 70,
                CaFrequency.WEEKLY, 6, "admin"));

        var sa = mock(StudentAssessment.class);
        lenient().when(sa.getId()).thenReturn("sa-1");
        when(studentAssessmentRepo.findAllBySubjectAndSessionAndTermId("subject-1", "session-1", "term-1"))
                .thenReturn(List.of(sa));

        score = mock(AssessmentScore.class);
        when(score.getCycle()).thenReturn(cycle);
        when(score.getId()).thenReturn("score-1");
        when(assessmentScoreRepo.findAllByStudentAssessmentIdAndAssessmentId("sa-1", "assess-1"))
                .thenReturn(List.of(score));
        lenient().when(structureService.freezeOpened(anyCollection())).thenReturn(List.of());
    }

    private List<AssessmentCaEntry> weeks(int count) {
        var list = new java.util.ArrayList<AssessmentCaEntry>();
        for (int i = 1; i <= count; i++) {
            list.add(AssessmentCaEntry.create(SCHOOL, "score-1", i, 70, "u1"));
        }
        return list;
    }

    @Test
    void theCaClosesOnceEverySlotIsRecorded() {
        when(caEntryRepo.findAllByScoreIds(List.of("score-1"))).thenReturn(weeks(6));

        useCase.execute(SCHOOL, "ts-1", "assess-1", "term-1");

        assertThat(cycle.isCaSubmitted()).isTrue();
        verify(cycleRepo).save(cycle);
    }

    @Test
    void aMissingWeekBlocksTheSubmission() {
        when(caEntryRepo.findAllByScoreIds(List.of("score-1"))).thenReturn(weeks(4));

        assertThatThrownBy(() -> useCase.execute(SCHOOL, "ts-1", "assess-1", "term-1"))
                .isInstanceOf(RuleException.class).hasMessageContaining("all 6 CA recordings")
                .hasMessageContaining("2 missing");

        assertThat(cycle.isCaSubmitted()).isFalse();
        verify(cycleRepo, never()).save(any(ClassSubjectAssessmentLifeCycle.class));
    }

    @Test
    void theCaCannotBeSubmittedTwice() {
        when(caEntryRepo.findAllByScoreIds(List.of("score-1"))).thenReturn(weeks(6));
        useCase.execute(SCHOOL, "ts-1", "assess-1", "term-1");

        assertThatThrownBy(() -> useCase.execute(SCHOOL, "ts-1", "assess-1", "term-1"))
                .isInstanceOf(RuleException.class).hasMessageContaining("already been submitted");
    }
}
