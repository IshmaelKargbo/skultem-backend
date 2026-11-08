package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.model.UserSession;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.repository.UserSessionRepository;
import com.moriba.skultem.domain.vo.Role;

class ChangePasswordUseCaseTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UserRepository userRepo;
    private SchoolUserRepository schoolUserRepo;
    private UserSessionRepository sessionRepo;
    private ChangePasswordUseCase useCase;
    private User user;
    private UserSession current;
    private UserSession other;

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepository.class);
        schoolUserRepo = mock(SchoolUserRepository.class);
        sessionRepo = mock(UserSessionRepository.class);
        useCase = new ChangePasswordUseCase(userRepo, schoolUserRepo, sessionRepo, encoder);

        user = User.create("Aminata", "Kamara", "a@school.sl", encoder.encode("temp123"), "hint");
        user.resetPassword(encoder.encode("oldpass1")); // now an active account
        when(userRepo.findById(user.getId())).thenReturn(Optional.of(user));

        current = UserSession.create("current", "SCL-1", user, "ip", "d", "Desktop", "os", "b", "ua");
        other = UserSession.create("other", "SCL-1", user, "ip", "d", "Phone", "os", "b", "ua");
        when(schoolUserRepo.findAllByUser_Id(user.getId())).thenReturn(List.of(SchoolUser.create("SCL-1", user, Role.TEACHER)));
        when(sessionRepo.findAllByUserAndSchoolIdAndActive(user.getId(), "SCL-1", true)).thenReturn(List.of(current, other));
    }

    @Test
    void changesPasswordAndSignsOutOtherDevicesOnly() {
        var res = useCase.execute(user.getId(), "current", "oldpass1", "newpass99");

        assertThat(encoder.matches("newpass99", user.getPassword())).isTrue();
        assertThat(res.otherSessionsSignedOut()).isEqualTo(1);
        assertThat(current.isActive()).isTrue();
        assertThat(other.isActive()).isFalse();
    }

    @Test
    void wrongCurrentPasswordIsRefusedWithoutSigningAnyoneOut() {
        assertThatThrownBy(() -> useCase.execute(user.getId(), "current", "nope", "newpass99"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("current password");
        assertThat(encoder.matches("oldpass1", user.getPassword())).isTrue();
        verify(userRepo, never()).save(any());
        assertThat(other.isActive()).isTrue();
    }

    @Test
    void newPasswordMustDifferAndBeLongEnough() {
        assertThatThrownBy(() -> useCase.execute(user.getId(), "current", "oldpass1", "oldpass1"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> useCase.execute(user.getId(), "current", "oldpass1", "abc"))
                .isInstanceOf(BadRequestException.class);
    }
}
