package com.moriba.skultem.application.services;

import static com.moriba.skultem.domain.vo.Level.DAYCARE;
import static com.moriba.skultem.domain.vo.Level.JSS;
import static com.moriba.skultem.domain.vo.Level.NURSERY;
import static com.moriba.skultem.domain.vo.Level.PRIMARY;
import static com.moriba.skultem.domain.vo.Level.SSS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import com.moriba.skultem.domain.model.StaffManagementSection;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.domain.vo.Role;

// Scenario 5 school: Early Years & Primary / JSS / SSS.
@ExtendWith(MockitoExtension.class)
class SectionScopeServiceTest {

    private static final String SCHOOL = "school-1";
    private static final String USER = "user-1";

    @Mock
    private SchoolRepository schoolRepo;
    @Mock
    private SchoolLevelRepository schoolLevelRepo;
    @Mock
    private ManagementSectionRepository managementSectionRepo;
    @Mock
    private StaffManagementSectionRepository staffSectionRepo;
    @InjectMocks
    private SectionScopeService service;

    private School school;
    private final ManagementSection early = ManagementSection.create(SCHOOL, "Early Years & Primary", 0);
    private final ManagementSection jss = ManagementSection.create(SCHOOL, "JSS", 1);
    private final ManagementSection sss = ManagementSection.create(SCHOOL, "SSS", 2);

    @BeforeEach
    void setUp() {
        school = School.create(SCHOOL, "King's Way", "kingsway", null, null);
        school.setManagementModel(ManagementModel.SECTION_BASED);
        lenient().when(schoolRepo.findById(SCHOOL)).thenReturn(Optional.of(school));

        List<SchoolLevel> levels = new ArrayList<>();
        levels.add(level(DAYCARE, early));
        levels.add(level(NURSERY, early));
        levels.add(level(PRIMARY, early));
        levels.add(level(JSS, jss));
        levels.add(level(SSS, sss));
        lenient().when(schoolLevelRepo.findBySchoolId(SCHOOL)).thenReturn(levels);
        lenient().when(managementSectionRepo.findBySchoolId(SCHOOL)).thenReturn(List.of(early, jss, sss));
        lenient().when(staffSectionRepo.findBySchoolAndUserAndRole(anyString(), anyString(), any())).thenReturn(List.of());
    }

    private SchoolLevel level(Level level, ManagementSection section) {
        var row = SchoolLevel.create(SCHOOL, level);
        row.assignTo(section.getId());
        return row;
    }

    private void assign(Role role, ManagementSection... sections) {
        List<StaffManagementSection> rows = new ArrayList<>();
        for (var s : sections) {
            rows.add(StaffManagementSection.create(SCHOOL, USER, role, s.getId()));
        }
        when(staffSectionRepo.findBySchoolAndUserAndRole(SCHOOL, USER, role)).thenReturn(rows);
    }

    // Scenario 6
    @Test
    void ownerAndProprietorAreAlwaysWholeSchool() {
        assertThat(service.resolve(SCHOOL, USER, Role.OWNER).wholeSchool()).isTrue();
        assertThat(service.resolve(SCHOOL, USER, Role.PROPRIETOR).wholeSchool()).isTrue();
        assertThat(service.resolve(SCHOOL, USER, Role.SUPER_ADMIN).wholeSchool()).isTrue();
    }

    // Scenario 7
    @Test
    void jssAdminOnlyReachesJss() {
        assign(Role.ADMIN, jss);

        var scope = service.resolve(SCHOOL, USER, Role.ADMIN);

        assertThat(scope.wholeSchool()).isFalse();
        assertThat(scope.levels()).containsExactly(JSS);
        assertThat(scope.allows(PRIMARY)).isFalse();
        assertThat(scope.allows(SSS)).isFalse();
        assertThat(scope.sectionNames()).containsExactly("JSS");
    }

    // Scenario 8
    @Test
    void accountantWithPrimaryAndJssCannotReachSss() {
        assign(Role.ACCOUNTANT, early, jss);

        var scope = service.resolve(SCHOOL, USER, Role.ACCOUNTANT);

        assertThat(scope.levels()).containsExactlyInAnyOrder(DAYCARE, NURSERY, PRIMARY, JSS);
        assertThat(scope.allows(SSS)).isFalse();
    }

    @Test
    void staffWithNoSectionsAreWholeSchool() {
        assertThat(service.resolve(SCHOOL, USER, Role.ADMIN).wholeSchool()).isTrue();
    }

    @Test
    void scopeIsPerRole() {
        assign(Role.ADMIN, jss);

        assertThat(service.resolve(SCHOOL, USER, Role.ADMIN).wholeSchool()).isFalse();
        assertThat(service.resolve(SCHOOL, USER, Role.TEACHER).wholeSchool()).isTrue();
    }

    @Test
    void unifiedSchoolIgnoresAnyLeftoverAssignments() {
        school.setManagementModel(ManagementModel.UNIFIED);
        lenient().when(staffSectionRepo.findBySchoolAndUserAndRole(SCHOOL, USER, Role.ADMIN))
                .thenReturn(List.of(StaffManagementSection.create(SCHOOL, USER, Role.ADMIN, jss.getId())));

        assertThat(service.resolve(SCHOOL, USER, Role.ADMIN).wholeSchool()).isTrue();
    }

    @Test
    void parentsAreNeverSectionScoped() {
        assertThat(service.resolve(SCHOOL, USER, Role.PARENT).wholeSchool()).isTrue();
    }

    // ---- The owner's "view one section" choice (X-View-Section) ----

    private void request(String sectionHeader, Role role) {
        var req = new org.springframework.mock.web.MockHttpServletRequest();
        if (sectionHeader != null) {
            req.addHeader(SectionScopeService.VIEW_HEADER, sectionHeader);
        }
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(req));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.moriba.skultem.infrastructure.security.AuthUser(USER, SCHOOL, role), null, List.of()));
    }

    @org.junit.jupiter.api.AfterEach
    void clearRequest() {
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void anOwnerViewingOneSectionSeesThatSectionsLevelsButStaysWholeSchool() {
        request(early.getId(), Role.OWNER);

        assertThat(service.view()).isPresent();
        assertThat(service.levels()).containsExactlyInAnyOrder(DAYCARE, NURSERY, PRIMARY);
        assertThat(service.restrictedLevels()).containsExactlyInAnyOrder(DAYCARE, NURSERY, PRIMARY);
        assertThat(service.effective().sectionIds()).containsExactly(early.getId());
        // A view is a filter, not a restriction - guards and the interceptor still see whole-school.
        assertThat(service.current().wholeSchool()).isTrue();
    }

    @Test
    void noHeaderMeansTheOwnersUsualWholeSchoolView() {
        request(null, Role.OWNER);

        assertThat(service.view()).isEmpty();
        assertThat(service.levels()).containsExactlyInAnyOrder(Level.values());
        assertThat(service.restrictedLevels()).isNull();
    }

    @Test
    void theHeaderIsIgnoredForAnyoneButOwnerLevelUsers() {
        request(early.getId(), Role.ADMIN);

        assertThat(service.view()).isEmpty();
        assertThat(service.levels()).containsExactlyInAnyOrder(Level.values());
    }

    @Test
    void anUnknownSectionIdIsIgnoredAndASectionLimitedUserCannotWidenOrSwitch() {
        request("not-a-section", Role.OWNER);
        assertThat(service.view()).isEmpty();

        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        assign(Role.ADMIN, jss);
        request(early.getId(), Role.ADMIN);
        assertThat(service.levels()).containsExactly(JSS);
    }

    @Test
    void aSchoolWithoutSectionsHasNothingToView() {
        school.setManagementModel(ManagementModel.UNIFIED);
        request(early.getId(), Role.OWNER);

        assertThat(service.view()).isEmpty();
    }
}
