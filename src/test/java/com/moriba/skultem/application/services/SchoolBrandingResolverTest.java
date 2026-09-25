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
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.Level;

@ExtendWith(MockitoExtension.class)
class SchoolBrandingResolverTest {

    private static final String SCHOOL = "school-1";

    @Mock
    private SchoolLevelRepository schoolLevelRepo;
    @Mock
    private ManagementSectionRepository managementSectionRepo;
    @Mock
    private EnrollmentRepository enrollmentRepo;
    @Mock
    private TeacherRepository teacherRepo;
    @Mock
    private PaymentRepository paymentRepo;
    @Mock
    private StaffManagementSectionRepository staffSectionRepo;
    @InjectMocks
    private SchoolBrandingResolver resolver;

    private School school;
    private final ManagementSection primary = ManagementSection.create(SCHOOL, "Primary", 0);
    private final ManagementSection secondary = ManagementSection.create(SCHOOL, "Secondary", 1);

    @BeforeEach
    void setUp() {
        school = School.create(SCHOOL, "Prospect", "prospect", new Address("West", "WA", null, "Freetown", "Main St"),
                null);
        school.updateBranding("school-logo", null, "Head Teacher", "school-sig", null, null);
        school.setManagementModel(ManagementModel.SECTION_BASED);

        lenient().when(schoolLevelRepo.findBySchoolId(SCHOOL)).thenReturn(List.of(
                new SchoolLevel("1", SCHOOL, Level.PRIMARY, primary.getId(), Instant.now(), Instant.now()),
                new SchoolLevel("2", SCHOOL, Level.JSS, secondary.getId(), Instant.now(), Instant.now())));
        lenient().when(managementSectionRepo.findBySchoolId(SCHOOL)).thenReturn(List.of(primary, secondary));
    }

    @Test
    void sectionOwnValuesWinAndBlankFieldsFallBackToTheSchool() {
        secondary.updateBranding("secondary-logo", "Mr Kamara", null, new Address(null, null, null, "Bo", "Hill Rd"));

        var b = resolver.forLevel(school, Level.JSS);

        assertThat(b.logo()).isEqualTo("secondary-logo");
        assertThat(b.principalName()).isEqualTo("Mr Kamara");
        assertThat(b.principalSignature()).isEqualTo("school-sig");
        assertThat(b.address().city()).isEqualTo("Bo");
    }

    @Test
    void aSectionWithNothingSetInheritsEverythingFromTheSchool() {
        var b = resolver.forLevel(school, Level.PRIMARY);

        assertThat(b.logo()).isEqualTo("school-logo");
        assertThat(b.principalName()).isEqualTo("Head Teacher");
        assertThat(b.address().city()).isEqualTo("Freetown");
    }

    @Test
    void aUnifiedSchoolIgnoresSectionValuesAndNoLevelMeansSchool() {
        secondary.updateBranding("secondary-logo", "Mr Kamara", null, null);

        assertThat(resolver.forLevel(school, null).logo()).isEqualTo("school-logo");

        school.setManagementModel(ManagementModel.UNIFIED);
        assertThat(resolver.forLevel(school, Level.JSS).logo()).isEqualTo("school-logo");
    }

    @Test
    void aStudentPrintsTheSectionOfTheirCurrentClass() {
        secondary.updateBranding("secondary-logo", null, null, null);
        var clazz = org.mockito.Mockito.mock(com.moriba.skultem.domain.model.Clazz.class);
        var enrollment = org.mockito.Mockito.mock(com.moriba.skultem.domain.model.Enrollment.class);
        org.mockito.Mockito.when(clazz.getLevel()).thenReturn(Level.JSS);
        org.mockito.Mockito.when(enrollment.getClazz()).thenReturn(clazz);
        org.mockito.Mockito.when(enrollmentRepo.findTopByStudentAndSchoolIdOrderByCreatedAtDesc("stu", SCHOOL))
                .thenReturn(java.util.Optional.of(enrollment));

        assertThat(resolver.forStudent(school, "stu").logo()).isEqualTo("secondary-logo");
        // No enrollment yet -> the school's own.
        assertThat(resolver.forStudent(school, "nobody").logo()).isEqualTo("school-logo");
    }

    @Test
    void aStaffMemberPrintsTheirSectionOnlyWhenLimitedToExactlyOne() {
        secondary.updateBranding("secondary-logo", null, null, null);
        var teacher = org.mockito.Mockito.mock(com.moriba.skultem.domain.model.Teacher.class,
                org.mockito.Mockito.RETURNS_DEEP_STUBS);
        org.mockito.Mockito.when(teacher.getUser().getId()).thenReturn("u1");
        org.mockito.Mockito.when(teacherRepo.findByIdAndSchoolId("t1", SCHOOL)).thenReturn(java.util.Optional.of(teacher));

        org.mockito.Mockito.when(staffSectionRepo.findBySchoolAndUserAndRole(SCHOOL, "u1",
                com.moriba.skultem.domain.vo.Role.TEACHER)).thenReturn(List.of(
                        com.moriba.skultem.domain.model.StaffManagementSection.create(SCHOOL, "u1",
                                com.moriba.skultem.domain.vo.Role.TEACHER, secondary.getId())));
        assertThat(resolver.forTeacher(school, "t1").logo()).isEqualTo("secondary-logo");

        org.mockito.Mockito.when(staffSectionRepo.findBySchoolAndUserAndRole(SCHOOL, "u1",
                com.moriba.skultem.domain.vo.Role.TEACHER)).thenReturn(List.of(
                        com.moriba.skultem.domain.model.StaffManagementSection.create(SCHOOL, "u1",
                                com.moriba.skultem.domain.vo.Role.TEACHER, secondary.getId()),
                        com.moriba.skultem.domain.model.StaffManagementSection.create(SCHOOL, "u1",
                                com.moriba.skultem.domain.vo.Role.TEACHER, primary.getId())));
        assertThat(resolver.forTeacher(school, "t1").logo()).isEqualTo("school-logo");
    }
}
