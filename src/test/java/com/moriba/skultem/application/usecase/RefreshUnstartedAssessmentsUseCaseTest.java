package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.application.services.AssessmentStructureService;
import com.moriba.skultem.domain.model.AssessmentCaEntry;
import com.moriba.skultem.domain.model.AssessmentConfiguration;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle.Status;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;
import com.moriba.skultem.domain.vo.Level;

// A section turning on Continuous Assessment expects it to apply now, not only after the next term - but
// only for assessments nobody has touched yet. Anything with a recorded score must stay exactly as it is.
@ExtendWith(MockitoExtension.class)
class RefreshUnstartedAssessmentsUseCaseTest {

    private static final String SCHOOL = "school-1";
    private static final String SECTION = "sec-secondary";

    @Mock
    private ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    @Mock
    private AssessmentScoreRepository scoreRepo;
    @Mock
    private AssessmentCaEntryRepository caEntryRepo;
    @Mock
    private AssessmentStructureService structureService;
    @InjectMocks
    private RefreshUnstartedAssessmentsUseCase useCase;

    private ClassSubjectAssessmentLifeCycle cycle(String id, Status status, boolean frozen) {
        var ts = mock(TeacherSubject.class, RETURNS_DEEP_STUBS);
        when(ts.getSession().getClazz().getLevel()).thenReturn(Level.SSS);
        var cycle = ClassSubjectAssessmentLifeCycle.create(id, SCHOOL, ts, mock(com.moriba.skultem.domain.model.Term.class),
                mock(com.moriba.skultem.domain.model.Assessment.class), status);
        if (frozen) {
            cycle.freezeStructure(AssessmentConfiguration.simple(SCHOOL, SECTION));
        }
        return cycle;
    }

    private AssessmentScore scoreGradedBy(String gradedByUserId) {
        var score = mock(AssessmentScore.class);
        // Unused when the score turns out to already be graded - isUntouched() short-circuits before
        // ever needing the id to look up CA entries.
        lenient().when(score.getId()).thenReturn("score-" + gradedByUserId);
        when(score.getGradedByUserId()).thenReturn(gradedByUserId);
        return score;
    }

    private AssessmentConfiguration caConfig() {
        return AssessmentConfiguration.create(SCHOOL, SECTION, AssessmentStructure.CA_AND_TEST, 30, 70,
                CaFrequency.WEEKLY, 6, "admin");
    }

    @BeforeEach
    void setUp() {
        when(structureService.levelsOfSection(SCHOOL, SECTION)).thenReturn(java.util.Set.of(Level.SSS));
        lenient().when(structureService.forLevel(SCHOOL, Level.SSS)).thenReturn(caConfig());
    }

    @Test
    void aBlankOpenAssessmentMovesOntoTheCurrentConfiguration() {
        var blank = cycle("blank", Status.DRAFT, true);
        when(cycleRepo.findAllOpenBySchool(SCHOOL)).thenReturn(List.of(blank));
        when(scoreRepo.findAllByCycle("blank")).thenReturn(List.of());

        var result = useCase.execute(SCHOOL, SECTION);

        assertThat(result.refreshed()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        assertThat(blank.isContinuous()).isTrue();
        assertThat(blank.getCaPercentage()).isEqualTo(30);
        verify(cycleRepo).saveAll(List.of(blank));
    }

    @Test
    void anAssessmentWithAGradedScoreIsLeftExactlyAsItIs() {
        var graded = cycle("graded", Status.DRAFT, true);
        when(cycleRepo.findAllOpenBySchool(SCHOOL)).thenReturn(List.of(graded));
        var score = scoreGradedBy("teacher-1");
        when(scoreRepo.findAllByCycle("graded")).thenReturn(List.of(score));

        var result = useCase.execute(SCHOOL, SECTION);

        assertThat(result.refreshed()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(graded.isContinuous()).isFalse();
        verify(cycleRepo, never()).saveAll(anyList());
    }

    @Test
    void anAssessmentWithARecordedCaEntryButNoGradedScoreIsStillLeftAlone() {
        var half = cycle("half", Status.DRAFT, true);
        when(cycleRepo.findAllOpenBySchool(SCHOOL)).thenReturn(List.of(half));
        var score = scoreGradedBy(null);
        when(scoreRepo.findAllByCycle("half")).thenReturn(List.of(score));
        when(caEntryRepo.findAllByScoreIds(List.of("score-null"))).thenReturn(List.of(mock(AssessmentCaEntry.class)));

        var result = useCase.execute(SCHOOL, SECTION);

        assertThat(result.refreshed()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(half.isContinuous()).isFalse();
    }

    @Test
    void anAssessmentOutsideTheSectionIsNeverTouched() {
        when(structureService.levelsOfSection(SCHOOL, SECTION)).thenReturn(java.util.Set.of(Level.PRIMARY));
        var blank = cycle("blank", Status.DRAFT, true);
        when(cycleRepo.findAllOpenBySchool(SCHOOL)).thenReturn(List.of(blank));

        var result = useCase.execute(SCHOOL, SECTION);

        assertThat(result.refreshed()).isZero();
        assertThat(result.skipped()).isZero();
        verify(scoreRepo, never()).findAllByCycle(anyString());
        verify(cycleRepo, never()).saveAll(anyList());
    }

    @Test
    void anAlreadyLockedAssessmentIsNeverConsidered() {
        var locked = cycle("locked", Status.LOCKED, true);
        when(cycleRepo.findAllOpenBySchool(SCHOOL)).thenReturn(List.of());

        var result = useCase.execute(SCHOOL, SECTION);

        assertThat(result.refreshed()).isZero();
        assertThat(result.skipped()).isZero();
        assertThat(locked.isContinuous()).isFalse();
    }
}
