package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;

import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.model.AssessmentCaEntry;
import com.moriba.skultem.domain.model.AssessmentConfiguration;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle.Status;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;
import com.moriba.skultem.infrastructure.security.PermissionService;

// A student's grades: the CA + formal test breakdown reaches a parent only once released, but school management can
// watch it while the teacher is still recording.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GradeReportUseCaseTest {

    @Mock
    private AssessmentScoreRepository repo;
    @Mock
    private SectionScopeService sectionScopeService;
    @Mock
    private ResolveScoreGradeUseCase resolveScoreGradeUseCase;
    @Mock
    private AssessmentCaEntryRepository caEntryRepo;
    @Mock
    private PermissionService permissionService;
    @InjectMocks
    private GradeReportUseCase useCase;

    private AssessmentScore inProgressScore() {
        var cycle = ClassSubjectAssessmentLifeCycle.create("c", "school", mock(com.moriba.skultem.domain.model.TeacherSubject.class),
                mock(com.moriba.skultem.domain.model.Term.class), mock(com.moriba.skultem.domain.model.Assessment.class),
                Status.DRAFT);
        cycle.freezeStructure(AssessmentConfiguration.create("school", "sec", AssessmentStructure.CA_AND_TEST, 30, 70,
                CaFrequency.WEEKLY, 3, "admin"));
        var score = mock(AssessmentScore.class, RETURNS_DEEP_STUBS);
        when(score.getId()).thenReturn("s1");
        when(score.getCycle()).thenReturn(cycle);
        when(score.getStatus()).thenReturn(Status.DRAFT);
        when(score.isApproved()).thenReturn(false);
        when(score.getCaScore()).thenReturn(60);
        when(score.getScore()).thenReturn(0);
        return score;
    }

    private ReportBuilderDTO request() {
        return new ReportBuilderDTO("school", null, List.of());
    }

    @Test
    void aParentDoesNotSeeACaBreakdownThatHasNotBeenReleased() {
        var page = new PageImpl<>(List.of(inProgressScore()));
        when(repo.runReport(any(), any(), any(), any())).thenReturn(page);
        when(permissionService.hasAnySchoolRole("school", "ADMIN", "PROPRIETOR", "OWNER")).thenReturn(false);

        var res = useCase.execute(request(), 1, 10);

        assertThat(res.getContent().get(0).continuous()).isNull();
    }

    @Test
    void managementSeesItWhileTheTeacherIsStillRecording() {
        var page = new PageImpl<>(List.of(inProgressScore()));
        when(repo.runReport(any(), any(), any(), any())).thenReturn(page);
        when(permissionService.hasAnySchoolRole("school", "ADMIN", "PROPRIETOR", "OWNER")).thenReturn(true);
        when(caEntryRepo.findAllByScoreIds(anyList())).thenReturn(List.of(
                AssessmentCaEntry.create("school", "s1", 1, 60, "u1")));

        var res = useCase.execute(request(), 1, 10);

        var continuous = res.getContent().get(0).continuous();
        assertThat(continuous).isNotNull();
        assertThat(continuous.caEntryScores().get(0)).isEqualTo(60);
        assertThat(continuous.caSubmitted()).isFalse();
    }

    @Test
    void aParentSeesTheBreakdownOnceTheTeacherHasSubmittedItAwaitingApproval() {
        var score = inProgressScore();
        when(score.getStatus()).thenReturn(Status.SUBMITTED);
        when(score.isSubmited()).thenReturn(true);
        var page = new PageImpl<>(List.of(score));
        when(repo.runReport(any(), any(), any(), any())).thenReturn(page);
        when(permissionService.hasAnySchoolRole("school", "ADMIN", "PROPRIETOR", "OWNER")).thenReturn(false);
        when(caEntryRepo.findAllByScoreIds(anyList())).thenReturn(List.of());

        var res = useCase.execute(request(), 1, 10);

        assertThat(res.getContent().get(0).continuous()).isNotNull();
    }
}
