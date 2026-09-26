package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.domain.model.AssessmentConfiguration;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle.Status;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.model.SchoolLevel;
import com.moriba.skultem.domain.repository.AssessmentConfigurationRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.vo.AssessmentStructure;
import com.moriba.skultem.domain.vo.CaFrequency;
import com.moriba.skultem.domain.vo.Level;

// The rule that matters: changing a section's configuration only affects assessments that open AFTER it.
@ExtendWith(MockitoExtension.class)
class AssessmentStructureServiceTest {

    private static final String SCHOOL = "school-1";
    private static final String PRIMARY = "sec-primary";

    @Mock
    private AssessmentConfigurationRepository configRepo;
    @Mock
    private SchoolRepository schoolRepo;
    @Mock
    private SchoolLevelRepository schoolLevelRepo;
    @InjectMocks
    private AssessmentStructureService service;

    private School school;

    @BeforeEach
    void setUp() {
        school = School.create(SCHOOL, "Prospect", "prospect", null, null);
        school.setManagementModel(ManagementModel.SECTION_BASED);
        lenient().when(schoolRepo.findById(SCHOOL)).thenReturn(Optional.of(school));
        var row = SchoolLevel.create(SCHOOL, Level.PRIMARY);
        row.assignTo(PRIMARY);
        lenient().when(schoolLevelRepo.findBySchoolId(SCHOOL)).thenReturn(List.of(row));
        lenient().when(configRepo.findBySection(SCHOOL, null)).thenReturn(Optional.empty());
    }

    private ClassSubjectAssessmentLifeCycle cycle(Status status) {
        var ts = mock(com.moriba.skultem.domain.model.TeacherSubject.class, RETURNS_DEEP_STUBS);
        when(ts.getSession().getClazz().getLevel()).thenReturn(Level.PRIMARY);
        return ClassSubjectAssessmentLifeCycle.create("c-" + status, SCHOOL, ts,
                mock(com.moriba.skultem.domain.model.Term.class), mock(com.moriba.skultem.domain.model.Assessment.class),
                status);
    }

    private AssessmentConfiguration ca(int caPct, int formalPct) {
        return AssessmentConfiguration.create(SCHOOL, PRIMARY, AssessmentStructure.CA_AND_TEST, caPct, formalPct,
                CaFrequency.WEEKLY, 6, "admin");
    }

    @Test
    void withNothingConfiguredAssessmentsStayTheSingleScoreTheyAlwaysWere() {
        assertThat(service.forLevel(SCHOOL, Level.PRIMARY).isContinuous()).isFalse();
    }

    @Test
    void aSectionUsesItsOwnConfigurationOtherwiseTheSchoolWideOne() {
        when(configRepo.findBySection(SCHOOL, PRIMARY)).thenReturn(Optional.of(ca(30, 70)));
        assertThat(service.forLevel(SCHOOL, Level.PRIMARY).getCaPercentage()).isEqualTo(30);

        when(configRepo.findBySection(SCHOOL, PRIMARY)).thenReturn(Optional.empty());
        when(configRepo.findBySection(SCHOOL, null)).thenReturn(Optional.of(ca(20, 80)));
        assertThat(service.forLevel(SCHOOL, Level.PRIMARY).getCaPercentage()).isEqualTo(20);
    }

    @Test
    void anAssessmentFreezesTheConfigurationInForceWhenItOpens() {
        when(configRepo.findBySection(SCHOOL, PRIMARY)).thenReturn(Optional.of(ca(30, 70)));
        var open = cycle(Status.DRAFT);

        var changed = service.freezeOpened(List.of(open));

        assertThat(changed).containsExactly(open);
        assertThat(open.isStructureFrozen()).isTrue();
        assertThat(open.isContinuous()).isTrue();
        assertThat(open.getCaPercentage()).isEqualTo(30);
        assertThat(open.getFormalPercentage()).isEqualTo(70);
        assertThat(open.getCaEntries()).isEqualTo(6);
    }

    @Test
    void changingTheConfigurationLaterNeverTouchesAnAssessmentThatAlreadyFroze() {
        var config = ca(30, 70);
        when(configRepo.findBySection(SCHOOL, PRIMARY)).thenReturn(Optional.of(config));
        var test1 = cycle(Status.DRAFT);
        service.freezeOpened(List.of(test1));

        // The administrator changes the section: CA 40 / Test 60, daily.
        config.change(AssessmentStructure.CA_AND_TEST, 40, 60, CaFrequency.MID_WEEK, 10, "admin");
        var changedAgain = service.freezeOpened(List.of(test1));

        assertThat(changedAgain).isEmpty();
        assertThat(test1.getCaPercentage()).isEqualTo(30);
        assertThat(test1.getFormalPercentage()).isEqualTo(70);
        assertThat(test1.getCaFrequency()).isEqualTo(CaFrequency.WEEKLY);
        assertThat(test1.getConfigVersion()).isEqualTo(1);

        // ...but an assessment that opens after the change gets the new structure.
        var test2 = cycle(Status.DRAFT);
        service.freezeOpened(List.of(test2));
        assertThat(test2.getCaPercentage()).isEqualTo(40);
        assertThat(test2.getCaFrequency()).isEqualTo(CaFrequency.MID_WEEK);
        assertThat(test2.getConfigVersion()).isEqualTo(2);
    }

    @Test
    void anAssessmentThatIsStillLockedWaitsForItsTurn() {
        lenient().when(configRepo.findBySection(SCHOOL, PRIMARY)).thenReturn(Optional.of(ca(30, 70)));
        var locked = cycle(Status.LOCKED);

        assertThat(service.freezeOpened(List.of(locked))).isEmpty();
        assertThat(locked.isStructureFrozen()).isFalse();
    }

    @Test
    void aSchoolWithoutSectionsUsesTheSchoolWideConfiguration() {
        school.setManagementModel(ManagementModel.UNIFIED);
        when(configRepo.findBySection(SCHOOL, null)).thenReturn(Optional.of(ca(25, 75)));

        assertThat(service.forLevel(SCHOOL, Level.PRIMARY).getCaPercentage()).isEqualTo(25);
    }

    @Test
    void levelsOfSectionReadsTheLevelsMappedToThatSection() {
        assertThat(service.levelsOfSection(SCHOOL, PRIMARY)).containsExactly(Level.PRIMARY);
        assertThat(service.levelsOfSection(SCHOOL, "sec-secondary")).isEmpty();
    }

    @Test
    void levelsOfSectionIsWholeSchoolForANullSectionOrAUnifiedSchool() {
        assertThat(service.levelsOfSection(SCHOOL, null)).isNull();

        school.setManagementModel(ManagementModel.UNIFIED);
        assertThat(service.levelsOfSection(SCHOOL, PRIMARY)).isNull();
    }
}
