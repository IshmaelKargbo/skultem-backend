package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.ParentPurgeRepository;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.Role;

@ExtendWith(MockitoExtension.class)
class ParentEditDeleteUseCasesTest {

    private static final String SCHOOL = "school-1";

    @Mock
    private ParentRepository parentRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private SchoolUserRepository schoolUserRepo;
    @Mock
    private LogActivityUseCase logActivityUseCase;
    @Mock
    private ParentPurgeRepository purgeRepo;

    private User user;
    private Parent parent;
    private EditParentUseCase edit;
    private DeleteParentPermanentlyUseCase delete;

    @BeforeEach
    void setUp() {
        user = User.create("Fatmata", "Sesay", "fatmata@example.com", "hash", "pw");
        parent = Parent.create(SCHOOL, "+23276000001", "12 Main Street", "Freetown", user);
        lenient().when(parentRepo.findByIdAndSchoolId(parent.getId(), SCHOOL)).thenReturn(Optional.of(parent));
        edit = new EditParentUseCase(parentRepo, userRepo, schoolUserRepo, logActivityUseCase);
        delete = new DeleteParentPermanentlyUseCase(parentRepo, purgeRepo, logActivityUseCase);
    }

    private SchoolUser membership(Role role, String school) {
        return SchoolUser.create(school, user, role);
    }

    @Test
    void editingUpdatesNameContactAndAddress() {
        edit.execute(SCHOOL, parent.getId(), " Fatmata B ", "Sesay", "+23276000009", "45 New Road", "Bo", null);

        assertThat(user.getGivenNames()).isEqualTo("Fatmata B");
        assertThat(parent.getPhone()).isEqualTo("+23276000009");
        assertThat(parent.getCity()).isEqualTo("Bo");
        verify(userRepo).save(user);
        verify(parentRepo).save(parent);
    }

    @Test
    void aPhoneAnotherParentUsesIsRejected() {
        when(parentRepo.existsByPhoneAndSchoolAndIdNot("+23276000009", SCHOOL, parent.getId())).thenReturn(true);

        assertThatThrownBy(() -> edit.execute(SCHOOL, parent.getId(), "Fatmata", "Sesay", "+23276000009",
                "45 New Road", "Bo", null)).isInstanceOf(AlreadyExistsException.class);
        verify(parentRepo, never()).save(parent);
    }

    @Test
    void anExistingEmailCanBeChangedWhenTheAccountIsOnlyThisParent() {
        when(schoolUserRepo.findAllByUser_Id(user.getId())).thenReturn(List.of(membership(Role.PARENT, SCHOOL)));
        when(userRepo.existsByEmail("new@example.com")).thenReturn(false);

        edit.execute(SCHOOL, parent.getId(), "Fatmata", "Sesay", "+23276000001", "12 Main Street", "Freetown",
                "new@example.com");

        assertThat(user.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void theEmailIsLockedWhenTheAccountIsAlsoUsedForAnotherRole() {
        when(schoolUserRepo.findAllByUser_Id(user.getId()))
                .thenReturn(List.of(membership(Role.PARENT, SCHOOL), membership(Role.TEACHER, SCHOOL)));

        assertThatThrownBy(() -> edit.execute(SCHOOL, parent.getId(), "Fatmata", "Sesay", "+23276000001",
                "12 Main Street", "Freetown", "new@example.com")).isInstanceOf(RuleException.class)
                .hasMessageContaining("another role");
        assertThat(user.getEmail()).isEqualTo("fatmata@example.com");
    }

    @Test
    void anEmailAnotherAccountOwnsIsRejected() {
        when(schoolUserRepo.findAllByUser_Id(user.getId())).thenReturn(List.of(membership(Role.PARENT, SCHOOL)));
        when(userRepo.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> edit.execute(SCHOOL, parent.getId(), "Fatmata", "Sesay", "+23276000001",
                "12 Main Street", "Freetown", "taken@example.com")).isInstanceOf(AlreadyExistsException.class);
    }

    @Test
    void aParentWithNoEmailIsPointedToAddEmail() {
        var noEmail = User.create("Ali", "Kamara", null, "hash", "pw");
        var p = Parent.create(SCHOOL, "+23276000002", "1 Some Street", "Bo", noEmail);
        when(parentRepo.findByIdAndSchoolId(p.getId(), SCHOOL)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> edit.execute(SCHOOL, p.getId(), "Ali", "Kamara", "+23276000002", "1 Some Street",
                "Bo", "ali@example.com")).isInstanceOf(RuleException.class).hasMessageContaining("Add Email");
    }

    @Test
    void deletingNeedsTheGuardiansFullName() {
        assertThatThrownBy(() -> delete.execute(SCHOOL, parent.getId(), "Fatmata"))
                .isInstanceOf(RuleException.class).hasMessageContaining("Fatmata Sesay");
        verify(purgeRepo, never()).purge(anyString(), anyString());
    }

    @Test
    void deletingIsRefusedWhileStudentsStillHaveThisGuardian() {
        when(purgeRepo.primaryStudentCount(SCHOOL, parent.getId())).thenReturn(2L);

        assertThatThrownBy(() -> delete.execute(SCHOOL, parent.getId(), "fatmata  SESAY"))
                .isInstanceOf(RuleException.class).hasMessageContaining("2 students");
        verify(purgeRepo, never()).purge(anyString(), anyString());
    }

    @Test
    void deletingPurgesTheGuardianWhenNoStudentDependsOnThem() {
        when(purgeRepo.primaryStudentCount(SCHOOL, parent.getId())).thenReturn(0L);
        when(purgeRepo.purge(SCHOOL, parent.getId())).thenReturn(
                new ParentPurgeRepository.Purged(1, ParentPurgeRepository.AccountOutcome.DELETED));

        var result = delete.execute(SCHOOL, parent.getId(), " Fatmata Sesay ");

        assertThat(result.parentName()).isEqualTo("Fatmata Sesay");
        assertThat(result.otherStudentLinksRemoved()).isEqualTo(1);
        assertThat(result.account()).isEqualTo("DELETED");
    }
}
