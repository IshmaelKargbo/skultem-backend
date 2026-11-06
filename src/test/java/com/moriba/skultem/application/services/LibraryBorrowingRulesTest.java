package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.moriba.skultem.domain.model.LibraryLoan.BorrowerType;
import com.moriba.skultem.domain.model.LibrarySetting;
import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.model.SchoolLevel;
import com.moriba.skultem.domain.model.StaffManagementSection;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.LibraryLoanRepository;
import com.moriba.skultem.domain.repository.LibrarySettingRepository;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.domain.vo.Owner;
import com.moriba.skultem.domain.vo.Role;

// Primary and Secondary can each run their own library: their own rules, their own borrowers, their own loan counts.
class LibraryBorrowingRulesTest {

    private static final String SCHOOL = "school-1";

    private final LibrarySettingRepository settingRepo = mock(LibrarySettingRepository.class);
    private final LibraryLoanRepository loanRepo = mock(LibraryLoanRepository.class);
    private final SchoolRepository schoolRepo = mock(SchoolRepository.class);
    private final SchoolLevelRepository levelRepo = mock(SchoolLevelRepository.class);
    private final ManagementSectionRepository sectionRepo = mock(ManagementSectionRepository.class);
    private final StaffManagementSectionRepository staffSectionRepo = mock(StaffManagementSectionRepository.class);
    private final TeacherRepository teacherRepo = mock(TeacherRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    private final ManagementSection primary = ManagementSection.create(SCHOOL, "Primary", 0);
    private final ManagementSection secondary = ManagementSection.create(SCHOOL, "Secondary", 1);

    private LibraryBorrowingRules rules;

    @BeforeEach
    void setUp() {
        var school = School.create(SCHOOL, "Prospect", "prospect",
                new Address("West", "WA", null, "Freetown", "Main St"),
                new Owner("Jane", "Doe", "jane@example.com", "+23276000000"));
        school.setManagementModel(ManagementModel.SECTION_BASED);
        when(schoolRepo.findById(SCHOOL)).thenReturn(Optional.of(school));
        when(sectionRepo.findBySchoolId(SCHOOL)).thenReturn(List.of(primary, secondary));
        when(levelRepo.findBySchoolId(SCHOOL)).thenReturn(List.of(
                new SchoolLevel("1", SCHOOL, Level.PRIMARY, primary.getId(), Instant.now(), Instant.now()),
                new SchoolLevel("2", SCHOOL, Level.JSS, secondary.getId(), Instant.now(), Instant.now())));
        rules = new LibraryBorrowingRules(settingRepo, loanRepo, schoolRepo, levelRepo, sectionRepo, staffSectionRepo,
                teacherRepo, jdbc);
    }

    private void studentEnrolledInSection(boolean enrolled) {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(enrolled ? 1L : 0L);
    }

    @Test
    void aSectionWithoutItsOwnRulesUsesTheSchools() {
        var schoolRules = LibrarySetting.defaults(SCHOOL);
        schoolRules.update(3, 6, false, 10);
        when(settingRepo.findBySchool(SCHOOL)).thenReturn(Optional.of(schoolRules));
        when(settingRepo.findBySchoolAndSection(SCHOOL, primary.getId())).thenReturn(Optional.empty());

        assertThat(rules.settingFor(SCHOOL, primary.getId()).getMaxStudentLoans()).isEqualTo(3);
    }

    @Test
    void aSectionsOwnRulesWin() {
        var own = LibrarySetting.forSection(primary.getId(), LibrarySetting.defaults(SCHOOL));
        own.update(1, 4, true, 7);
        when(settingRepo.findBySchoolAndSection(SCHOOL, primary.getId())).thenReturn(Optional.of(own));

        assertThat(rules.settingFor(SCHOOL, primary.getId()).getDefaultLoanDays()).isEqualTo(7);
    }

    @Test
    void aStudentFromAnotherSectionCantBorrowThisSectionsBook() {
        studentEnrolledInSection(false);

        assertThat(rules.outsideSectionReason(SCHOOL, BorrowerType.STUDENT, "stu-1", primary.getId()))
                .contains("Primary");
    }

    @Test
    void aStudentInTheSectionCanBorrow() {
        studentEnrolledInSection(true);

        assertThat(rules.outsideSectionReason(SCHOOL, BorrowerType.STUDENT, "stu-1", primary.getId())).isNull();
    }

    @Test
    void aTeacherLimitedToAnotherSectionCantBorrowButAWholeSchoolTeacherCan() {
        var user = mock(User.class);
        when(user.getId()).thenReturn("user-1");
        var teacher = mock(Teacher.class);
        when(teacher.getUser()).thenReturn(user);
        when(teacherRepo.findByIdAndSchoolId("t-1", SCHOOL)).thenReturn(Optional.of(teacher));

        when(staffSectionRepo.findBySchoolAndUserAndRole(SCHOOL, "user-1", Role.TEACHER))
                .thenReturn(List.of(StaffManagementSection.create(SCHOOL, "user-1", Role.TEACHER, secondary.getId())));
        assertThat(rules.outsideSectionReason(SCHOOL, BorrowerType.TEACHER, "t-1", primary.getId())).isNotNull();

        when(staffSectionRepo.findBySchoolAndUserAndRole(SCHOOL, "user-1", Role.TEACHER)).thenReturn(List.of());
        assertThat(rules.outsideSectionReason(SCHOOL, BorrowerType.TEACHER, "t-1", primary.getId())).isNull();
    }

    @Test
    void loanLimitsCountOnlyThatSectionsBooks() {
        studentEnrolledInSection(true);
        var own = LibrarySetting.forSection(primary.getId(), LibrarySetting.defaults(SCHOOL));
        own.update(1, 4, true, 7);
        when(settingRepo.findBySchoolAndSection(SCHOOL, primary.getId())).thenReturn(Optional.of(own));
        when(loanRepo.countActiveByBorrowerInSection("stu-1", SCHOOL, primary.getId())).thenReturn(0L);
        when(loanRepo.countActiveByBorrower("stu-1", SCHOOL)).thenReturn(5L);

        assertThat(rules.blockReason(SCHOOL, BorrowerType.STUDENT, "stu-1", primary.getId(), LocalDate.now()))
                .isNull();
    }
}
