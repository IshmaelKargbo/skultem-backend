package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.repository.CommunicationAudienceRepository;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.domain.vo.SectionScope;
import com.moriba.skultem.infrastructure.security.AuthUser;

// Prospect Academy: a Primary section and a Secondary section. A Primary announcement must reach Primary's
// people (and whole-school staff), not Secondary's - and a Primary admin must not post for Secondary.
@ExtendWith(MockitoExtension.class)
class CommunicationScopeServiceTest {

    private static final String SCHOOL = "school-1";

    @Mock
    private SchoolRepository schoolRepo;
    @Mock
    private ManagementSectionRepository sectionRepo;
    @Mock
    private SectionScopeService sectionScopeService;
    @Mock
    private CommunicationAudienceRepository audienceRepo;
    @InjectMocks
    private CommunicationScopeService service;

    private final ManagementSection primary = ManagementSection.create(SCHOOL, "Primary", 0);
    private final ManagementSection secondary = ManagementSection.create(SCHOOL, "Secondary", 1);
    private School school;

    @BeforeEach
    void setUp() {
        school = School.create(SCHOOL, "Prospect", "prospect", null, null);
        school.setManagementModel(ManagementModel.SECTION_BASED);
        lenient().when(schoolRepo.findById(SCHOOL)).thenReturn(Optional.of(school));
        lenient().when(sectionRepo.findBySchoolId(SCHOOL)).thenReturn(List.of(primary, secondary));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void signIn(Role role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthUser("user-1", SCHOOL, role), null, List.of()));
    }

    private SectionScope limitedTo(ManagementSection... sections) {
        return SectionScope.sections(EnumSet.of(Level.PRIMARY),
                java.util.Arrays.stream(sections).map(ManagementSection::getId).toList(),
                java.util.Arrays.stream(sections).map(ManagementSection::getName).toList());
    }

    @Test
    void aSchoolWithoutSectionsAddressesEveryoneAndFiltersNothing() {
        school.setManagementModel(ManagementModel.UNIFIED);

        assertThat(service.resolveTarget(SCHOOL, primary.getId())).isNull();
        assertThat(service.visibleSectionIds(SCHOOL)).isNull();
        assertThat(service.canSee(SCHOOL, primary.getId())).isTrue();
    }

    @Test
    void aSectionLimitedAdminPostsToTheirOwnSectionByDefaultAndNeverToAnother() {
        when(sectionScopeService.current()).thenReturn(limitedTo(primary));

        assertThat(service.resolveTarget(SCHOOL, null)).isEqualTo(primary.getId());
        assertThat(service.resolveTarget(SCHOOL, primary.getId())).isEqualTo(primary.getId());
        assertThatThrownBy(() -> service.resolveTarget(SCHOOL, secondary.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void anAdminLimitedToSeveralSectionsMustSayWhichOne() {
        when(sectionScopeService.current()).thenReturn(limitedTo(primary, secondary));

        assertThatThrownBy(() -> service.resolveTarget(SCHOOL, null)).isInstanceOf(RuleException.class)
                .hasMessageContaining("Choose which section");
        assertThat(service.resolveTarget(SCHOOL, secondary.getId())).isEqualTo(secondary.getId());
    }

    @Test
    void wholeSchoolStaffMayPostToTheWholeSchoolOrOneSection() {
        when(sectionScopeService.current()).thenReturn(SectionScope.all());

        assertThat(service.resolveTarget(SCHOOL, null)).isNull();
        assertThat(service.resolveTarget(SCHOOL, primary.getId())).isEqualTo(primary.getId());
        assertThatThrownBy(() -> service.resolveTarget(SCHOOL, "no-such-section")).isInstanceOf(RuleException.class);
    }

    @Test
    void whoSeesWhat() {
        // A section-limited Primary teacher: whole-school + Primary, not Secondary.
        when(sectionScopeService.currentOrAll()).thenReturn(limitedTo(primary));
        assertThat(service.visibleSectionIds(SCHOOL)).containsExactly(primary.getId());
        assertThat(service.canSee(SCHOOL, null)).isTrue();
        assertThat(service.canSee(SCHOOL, primary.getId())).isTrue();
        assertThat(service.canSee(SCHOOL, secondary.getId())).isFalse();
    }

    @Test
    void aParentSeesTheSectionsTheirChildrenAreIn() {
        when(sectionScopeService.currentOrAll()).thenReturn(SectionScope.all());
        when(audienceRepo.sectionIdsOfChildren(SCHOOL, "user-1")).thenReturn(Set.of(primary.getId()));
        signIn(Role.PARENT);

        assertThat(service.visibleSectionIds(SCHOOL)).containsExactly(primary.getId());
        assertThat(service.canSee(SCHOOL, secondary.getId())).isFalse();
    }

    @Test
    void wholeSchoolStaffSeeEverything() {
        when(sectionScopeService.currentOrAll()).thenReturn(SectionScope.all());
        signIn(Role.OWNER);

        assertThat(service.visibleSectionIds(SCHOOL)).isNull();
        assertThat(service.canSee(SCHOOL, secondary.getId())).isTrue();
    }
}
