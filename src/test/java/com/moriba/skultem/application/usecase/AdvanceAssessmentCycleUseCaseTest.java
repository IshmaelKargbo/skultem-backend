package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.AssessmentStructureService;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Assessment;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle.Status;
import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.vo.Level;

// Primary and Secondary are managed separately: moving Primary on to Test 2 must leave Secondary on Test 1, and the
// term only closes once every section has finished its last assessment.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdvanceAssessmentCycleUseCaseTest {

    private static final String SCHOOL = "school-1";

    @Mock
    private TermRepository termRepository;
    @Mock
    private ClassSubjectAssessmentLifeCycleRepository cycleRepository;
    @Mock
    private AcademicYearRepository academicYearRepo;
    @Mock
    private AssessmentStructureService structureService;
    @Mock
    private SchoolRepository schoolRepo;
    @Mock
    private ManagementSectionRepository sectionRepo;
    @InjectMocks
    private AdvanceAssessmentCycleUseCase useCase;

    private Term term;
    private final ManagementSection primary = ManagementSection.create(SCHOOL, "Primary", 0);
    private final ManagementSection secondary = ManagementSection.create(SCHOOL, "Secondary", 1);

    @BeforeEach
    void setUp() {
        var school = School.create(SCHOOL, "Prospect", "prospect", null, null);
        school.setManagementModel(ManagementModel.SECTION_BASED);
        when(schoolRepo.findById(SCHOOL)).thenReturn(Optional.of(school));
        when(sectionRepo.findBySchoolId(SCHOOL)).thenReturn(List.of(primary, secondary));
        when(structureService.levelsOfSection(SCHOOL, primary.getId())).thenReturn(Set.of(Level.PRIMARY));
        when(structureService.levelsOfSection(SCHOOL, secondary.getId())).thenReturn(Set.of(Level.SSS));

        var year = mock(AcademicYear.class);
        when(year.getId()).thenReturn("ay");
        when(academicYearRepo.findActiveBySchool(SCHOOL)).thenReturn(Optional.of(year));
        term = mock(Term.class);
        when(term.getTermNumber()).thenReturn(1);
        when(termRepository.findByIdAndAcademicYearIdAndSchoolId("t1", "ay", SCHOOL)).thenReturn(Optional.of(term));
        when(termRepository.findByTernNumberAndAcademicYearIdAndSchoolId(2, "ay", SCHOOL)).thenReturn(Optional.empty());
    }

    private ClassSubjectAssessmentLifeCycle cycle(String id, Level level, int position, Status status) {
        var ts = mock(TeacherSubject.class, RETURNS_DEEP_STUBS);
        when(ts.getSession().getClazz().getLevel()).thenReturn(level);
        var assessment = mock(Assessment.class);
        when(assessment.getPosition()).thenReturn(position);
        return ClassSubjectAssessmentLifeCycle.create(id, SCHOOL, ts, term, assessment, status);
    }

    @Test
    void movingOneSectionLeavesTheOtherOnItsCurrentAssessment() {
        var p1 = cycle("p1", Level.PRIMARY, 1, Status.APPROVED);
        var p2 = cycle("p2", Level.PRIMARY, 2, Status.LOCKED);
        var s1 = cycle("s1", Level.SSS, 1, Status.DRAFT);
        var s2 = cycle("s2", Level.SSS, 2, Status.LOCKED);
        when(cycleRepository.findAllBySchoolAndTerm(SCHOOL, "t1")).thenReturn(List.of(p1, p2, s1, s2));

        var result = useCase.execute(SCHOOL, "t1", primary.getId());

        assertThat(result.advanced()).isTrue();
        assertThat(result.sectionName()).isEqualTo("Primary");
        assertThat(p1.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(p2.getStatus()).isEqualTo(Status.DRAFT);
        assertThat(s1.getStatus()).isEqualTo(Status.DRAFT);
        assertThat(s2.getStatus()).isEqualTo(Status.LOCKED);
    }

    @Test
    void aSectionCannotMoveWhileItsOwnAssessmentsAreStillPendingApproval() {
        var p1 = cycle("p1", Level.PRIMARY, 1, Status.SUBMITTED);
        var s1 = cycle("s1", Level.SSS, 1, Status.APPROVED);
        when(cycleRepository.findAllBySchoolAndTerm(SCHOOL, "t1")).thenReturn(List.of(p1, s1));

        assertThatThrownBy(() -> useCase.execute(SCHOOL, "t1", primary.getId())).isInstanceOf(RuleException.class)
                .hasMessageContaining("pending approval");
        assertThat(s1.getStatus()).isEqualTo(Status.APPROVED);
    }

    @Test
    void aSectionOnItsLastAssessmentWaitsForTheOthersBeforeTheTermCloses() {
        var p1 = cycle("p1", Level.PRIMARY, 1, Status.APPROVED);
        var s1 = cycle("s1", Level.SSS, 1, Status.DRAFT);
        when(cycleRepository.findAllBySchoolAndTerm(SCHOOL, "t1")).thenReturn(List.of(p1, s1));

        var result = useCase.execute(SCHOOL, "t1", primary.getId());

        assertThat(result.sectionCompleted()).isTrue();
        assertThat(result.completed()).isFalse();
        assertThat(p1.getStatus()).isEqualTo(Status.COMPLETED);
        verify(term, never()).lock();
    }

    @Test
    void theTermClosesWhenTheLastSectionFinishes() {
        var p1 = cycle("p1", Level.PRIMARY, 1, Status.COMPLETED);
        var s1 = cycle("s1", Level.SSS, 1, Status.APPROVED);
        when(cycleRepository.findAllBySchoolAndTerm(SCHOOL, "t1")).thenReturn(List.of(p1, s1));

        var result = useCase.execute(SCHOOL, "t1", secondary.getId());

        assertThat(result.completed()).isTrue();
        verify(term).lock();
    }

    @Test
    void aSchoolWithSectionsMustSayWhichSection() {
        assertThatThrownBy(() -> useCase.execute(SCHOOL, "t1", null)).isInstanceOf(RuleException.class)
                .hasMessageContaining("Choose which section");
        verify(cycleRepository, never()).saveAll(any());
    }
}
