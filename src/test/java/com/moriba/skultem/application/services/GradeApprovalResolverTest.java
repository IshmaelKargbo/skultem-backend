package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.model.SchoolLevel;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.GradeApprover;
import com.moriba.skultem.domain.vo.Level;

@ExtendWith(MockitoExtension.class)
class GradeApprovalResolverTest {

    private static final String SCHOOL = "school-1";

    @Mock
    private SchoolLevelRepository schoolLevelRepo;
    @Mock
    private ManagementSectionRepository managementSectionRepo;
    @InjectMocks
    private GradeApprovalResolver resolver;

    private School school;
    private final ManagementSection primary = ManagementSection.create(SCHOOL, "Primary", 0);
    private final ManagementSection secondary = ManagementSection.create(SCHOOL, "Secondary", 1);

    @BeforeEach
    void setUp() {
        school = School.create(SCHOOL, "Prospect", "prospect", new Address("West", "WA", null, "Freetown", "Main St"),
                null);
        school.setManagementModel(ManagementModel.SECTION_BASED);

        lenient().when(schoolLevelRepo.findBySchoolId(SCHOOL)).thenReturn(List.of(
                new SchoolLevel("1", SCHOOL, Level.PRIMARY, primary.getId(), Instant.now(), Instant.now()),
                new SchoolLevel("2", SCHOOL, Level.JSS, secondary.getId(), Instant.now(), Instant.now())));
        lenient().when(managementSectionRepo.findBySchoolId(SCHOOL)).thenReturn(List.of(primary, secondary));
    }

    @Test
    void aNewSchoolHasTheClassMasterApprove() {
        assertThat(resolver.forLevel(school, Level.PRIMARY)).isEqualTo(GradeApprover.CLASS_MASTER);
    }

    @Test
    void theSchoolsChoiceAppliesToEverySectionThatDoesNotSetItsOwn() {
        school.updateGradeApprover(GradeApprover.ADMIN);
        assertThat(resolver.forLevel(school, Level.PRIMARY)).isEqualTo(GradeApprover.ADMIN);
        assertThat(resolver.forLevel(school, Level.JSS)).isEqualTo(GradeApprover.ADMIN);
    }

    @Test
    void aSectionCanDifferFromTheSchool() {
        // Primary has class masters approve; secondary has an admin do it.
        school.updateGradeApprover(GradeApprover.CLASS_MASTER);
        secondary.updateGradeApprover(GradeApprover.ADMIN);

        assertThat(resolver.forLevel(school, Level.PRIMARY)).isEqualTo(GradeApprover.CLASS_MASTER);
        assertThat(resolver.forLevel(school, Level.JSS)).isEqualTo(GradeApprover.ADMIN);
    }

    @Test
    void clearingASectionsChoiceGoesBackToTheSchools() {
        school.updateGradeApprover(GradeApprover.CLASS_MASTER);
        secondary.updateGradeApprover(GradeApprover.ADMIN);
        secondary.updateGradeApprover(null);

        assertThat(resolver.forLevel(school, Level.JSS)).isEqualTo(GradeApprover.CLASS_MASTER);
    }

    @Test
    void aSchoolWithoutSectionsIgnoresSectionValuesAndNoLevelMeansTheSchool() {
        secondary.updateGradeApprover(GradeApprover.ADMIN);
        school.setManagementModel(ManagementModel.UNIFIED);

        assertThat(resolver.forLevel(school, Level.JSS)).isEqualTo(GradeApprover.CLASS_MASTER);
        assertThat(resolver.forLevel(school, null)).isEqualTo(GradeApprover.CLASS_MASTER);
    }
}
