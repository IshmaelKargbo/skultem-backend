package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.vo.Role;

@ExtendWith(MockitoExtension.class)
class AssignStaffManagementSectionsUseCaseTest {

    private static final String SCHOOL = "school-1";
    private static final String USER = "user-1";

    @Mock
    private SchoolRepository schoolRepo;
    @Mock
    private SchoolUserRepository schoolUserRepo;
    @Mock
    private ManagementSectionRepository managementSectionRepo;
    @Mock
    private StaffManagementSectionRepository staffSectionRepo;
    @Mock
    private LogActivityUseCase logActivityUseCase;
    @InjectMocks
    private AssignStaffManagementSectionsUseCase useCase;

    private School school;
    private final ManagementSection jss = ManagementSection.create(SCHOOL, "JSS", 0);
    private final ManagementSection sss = ManagementSection.create(SCHOOL, "SSS", 1);

    @BeforeEach
    void setUp() {
        school = School.create(SCHOOL, "King's Way", "kingsway", null, null);
        school.setManagementModel(ManagementModel.SECTION_BASED);
        lenient().when(schoolRepo.findById(SCHOOL)).thenReturn(Optional.of(school));
        lenient().when(managementSectionRepo.findBySchoolId(SCHOOL)).thenReturn(List.of(jss, sss));
        var user = User.create("Ama", "Kamara", "ama@example.com", "hash", "hint");
        lenient().when(schoolUserRepo.findBySchoolAndUserAndRole(anyString(), anyString(), any()))
                .thenReturn(Optional.of(SchoolUser.create(SCHOOL, user, Role.ADMIN)));
    }

    @Test
    void limitsAnAdminToTheirSections() {
        var res = useCase.execute(SCHOOL, USER, Role.ADMIN, List.of(sss.getId(), jss.getId()));

        // Stored in the school's section order, not request order.
        assertThat(res.sectionIds()).containsExactly(jss.getId(), sss.getId());
        verify(staffSectionRepo).replace(SCHOOL, USER, Role.ADMIN, List.of(jss.getId(), sss.getId()));
    }

    @Test
    void emptyListRestoresWholeSchool() {
        var res = useCase.execute(SCHOOL, USER, Role.ADMIN, List.of());

        assertThat(res.sectionIds()).isEmpty();
        verify(staffSectionRepo).replace(SCHOOL, USER, Role.ADMIN, List.of());
    }

    @Test
    void rejectsASectionFromAnotherSchool() {
        var foreign = ManagementSection.create("other-school", "Theirs", 0);

        assertThatThrownBy(() -> useCase.execute(SCHOOL, USER, Role.ADMIN, List.of(foreign.getId())))
                .isInstanceOf(RuleException.class).hasMessageContaining("doesn't belong to this school");
        verify(staffSectionRepo, never()).replace(anyString(), anyString(), any(), anyList());
    }

    @Test
    void ownerLevelRolesCannotBeScoped() {
        assertThatThrownBy(() -> useCase.execute(SCHOOL, USER, Role.OWNER, List.of(jss.getId())))
                .isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> useCase.execute(SCHOOL, USER, Role.PROPRIETOR, List.of(jss.getId())))
                .isInstanceOf(RuleException.class);
        assertThatThrownBy(() -> useCase.execute(SCHOOL, USER, Role.PARENT, List.of(jss.getId())))
                .isInstanceOf(RuleException.class);
    }

    @Test
    void unifiedSchoolCannotScopeStaff() {
        school.setManagementModel(ManagementModel.UNIFIED);

        assertThatThrownBy(() -> useCase.execute(SCHOOL, USER, Role.ADMIN, List.of(jss.getId())))
                .isInstanceOf(RuleException.class).hasMessageContaining("Set up management sections first");
    }

    @Test
    void userMustHoldTheRole() {
        lenient().when(schoolUserRepo.findBySchoolAndUserAndRole(SCHOOL, USER, Role.ACCOUNTANT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(SCHOOL, USER, Role.ACCOUNTANT, List.of(jss.getId())))
                .isInstanceOf(NotFoundException.class);
    }
}
