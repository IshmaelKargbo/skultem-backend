package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.model.AssessmentCaEntry;
import com.moriba.skultem.domain.model.AssessmentConfiguration;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle.Status;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;
import com.moriba.skultem.domain.vo.Level;

// The CA report flags a student from the classwork while the assessment is still open - a low or falling average
// shows up before the formal test, when help is still cheap.
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ContinuousAssessmentReportUseCaseTest {

    private static final String SCHOOL = "school-1";

    @Mock
    private ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    @Mock
    private AssessmentScoreRepository scoreRepo;
    @Mock
    private AssessmentCaEntryRepository caEntryRepo;
    @Mock
    private SectionScopeService sectionScopeService;
    @InjectMocks
    private ContinuousAssessmentReportUseCase useCase;

    private ClassSubjectAssessmentLifeCycle cycle() {
        var ts = mock(com.moriba.skultem.domain.model.TeacherSubject.class, RETURNS_DEEP_STUBS);
        when(ts.getSession().getClazz().getLevel()).thenReturn(Level.SSS);
        when(ts.getSession().getClazz().getName()).thenReturn("SSS 1");
        when(ts.getSubject().getName()).thenReturn("Math");
        when(ts.getTeacher().getUser().getName()).thenReturn("Mr Kamara");
        var assessment = mock(com.moriba.skultem.domain.model.Assessment.class, RETURNS_DEEP_STUBS);
        when(assessment.getName()).thenReturn("Test 1");
        when(assessment.getTemplate().getPassMark()).thenReturn(50);
        var cycle = ClassSubjectAssessmentLifeCycle.create("cycle-1", SCHOOL, ts,
                mock(com.moriba.skultem.domain.model.Term.class), assessment, Status.DRAFT);
        cycle.freezeStructure(AssessmentConfiguration.create(SCHOOL, "sec", AssessmentStructure.CA_AND_TEST, 30, 70,
                CaFrequency.WEEKLY, 3, "admin"));
        return cycle;
    }

    private AssessmentScore student(String id, String name, Integer ca, Integer formal) {
        var score = mock(AssessmentScore.class, RETURNS_DEEP_STUBS);
        lenient().when(score.getId()).thenReturn(id);
        lenient().when(score.getCaScore()).thenReturn(ca);
        lenient().when(score.getFormalScore()).thenReturn(formal);
        lenient().when(score.getStudentAssessment().getEnrollment().getStudent().getName()).thenReturn(name);
        return score;
    }

    private AssessmentCaEntry entry(String scoreId, int n, int value) {
        return AssessmentCaEntry.create(SCHOOL, scoreId, n, value, "u1");
    }

    @Test
    void aFallingOrLowClassworkAverageIsFlaggedBeforeTheFormalTest() {
        var cycle = cycle();
        var falling = student("s1", "Ada", 63, null);
        var low = student("s2", "Bo", 40, null);
        var fine = student("s3", "Cy", 82, 80);
        when(cycleRepo.findAllBySchoolAndTerm(SCHOOL, "term-1")).thenReturn(List.of(cycle));
        when(scoreRepo.findAllByCycle("cycle-1")).thenReturn(List.of(falling, low, fine));
        when(caEntryRepo.findAllByScoreIds(List.of("s1", "s2", "s3"))).thenReturn(List.of(
                entry("s1", 1, 90), entry("s1", 2, 65), entry("s1", 3, 35),
                entry("s2", 1, 40), entry("s2", 2, 42), entry("s2", 3, 38),
                entry("s3", 1, 80), entry("s3", 2, 82), entry("s3", 3, 84)));
        when(sectionScopeService.levels()).thenReturn(null);

        var report = useCase.execute(SCHOOL, "term-1", null, null);

        assertThat(report.summary().assessments()).isEqualTo(1);
        assertThat(report.summary().flagged()).isEqualTo(2);
        assertThat(report.students()).extracting(s -> s.student()).containsExactlyInAnyOrder("Ada", "Bo");
        assertThat(report.students().stream().filter(s -> s.student().equals("Ada")).findFirst().get().reasons())
                .anyMatch(r -> r.contains("trending down"));
        assertThat(report.students().stream().filter(s -> s.student().equals("Bo")).findFirst().get().reasons())
                .anyMatch(r -> r.contains("below the pass mark"));
        assertThat(report.assessments().get(0).recordedPercent()).isEqualTo(100);
        assertThat(report.assessments().get(0).declining()).isEqualTo(1);
    }

    @Test
    void aFormalTestFarBelowTheClassworkIsFlaggedToo() {
        var cycle = cycle();
        var s = student("s1", "Di", 85, 55);
        when(cycleRepo.findAllBySchoolAndTerm(SCHOOL, "term-1")).thenReturn(List.of(cycle));
        when(scoreRepo.findAllByCycle("cycle-1")).thenReturn(List.of(s));
        when(caEntryRepo.findAllByScoreIds(List.of("s1"))).thenReturn(List.of(entry("s1", 1, 85)));
        when(sectionScopeService.levels()).thenReturn(null);

        var report = useCase.execute(SCHOOL, "term-1", null, null);

        assertThat(report.students()).hasSize(1);
        assertThat(report.students().get(0).reasons()).anyMatch(r -> r.contains("well below classwork"));
    }

    @Test
    void aSectionLimitedCallerOnlySeesTheirOwnLevels() {
        var cycle = cycle();
        when(cycleRepo.findAllBySchoolAndTerm(SCHOOL, "term-1")).thenReturn(List.of(cycle));
        when(sectionScopeService.levels()).thenReturn(java.util.EnumSet.of(Level.PRIMARY));

        var report = useCase.execute(SCHOOL, "term-1", null, null);

        assertThat(report.summary().assessments()).isZero();
        assertThat(report.students()).isEmpty();
    }

    @Test
    void theFlaggedStudentsArePagedAndTheTotalIsKept() {
        var cycle = cycle();
        var a = student("s1", "Ada", 40, null);
        var b = student("s2", "Bo", 45, null);
        var c = student("s3", "Cy", 30, null);
        when(cycleRepo.findAllBySchoolAndTerm(SCHOOL, "term-1")).thenReturn(List.of(cycle));
        when(scoreRepo.findAllByCycle("cycle-1")).thenReturn(List.of(a, b, c));
        when(caEntryRepo.findAllByScoreIds(List.of("s1", "s2", "s3"))).thenReturn(List.of(
                entry("s1", 1, 40), entry("s2", 1, 45), entry("s3", 1, 30)));
        when(sectionScopeService.levels()).thenReturn(null);

        var first = useCase.execute(SCHOOL, "term-1", null, null, 1, 2);
        var second = useCase.execute(SCHOOL, "term-1", null, null, 2, 2);

        assertThat(first.studentsTotal()).isEqualTo(3);
        assertThat(first.students()).hasSize(2);
        assertThat(second.students()).hasSize(1);
        assertThat(second.page()).isEqualTo(2);
        // Lowest classwork first.
        assertThat(first.students().get(0).student()).isEqualTo("Cy");
    }
}
