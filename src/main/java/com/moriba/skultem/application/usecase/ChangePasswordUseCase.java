package com.moriba.skultem.application.usecase;

import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.repository.UserSessionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A signed-in user changing their own password. They must give the current one, so an unattended open
// session can't be used to take the account over. Every other device is signed out; this one stays in.
@Service
@Transactional
@RequiredArgsConstructor
public class ChangePasswordUseCase {

    private final UserRepository userRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final UserSessionRepository sessionRepo;
    private final PasswordEncoder passwordEncoder;

    public record Result(int otherSessionsSignedOut) {
    }

    @AuditLogAnnotation(action = "CHANGE_PASSWORD")
    public Result execute(String userId, String currentSessionId, String currentPassword, String newPassword) {
        var user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        // 400, not 401: a wrong current password must not look like an expired session (that logs them out).
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Your current password is not correct");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("The new password must be at least 6 characters");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("Choose a password different from your current one");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // Whoever else had this password is signed out, at every school this account belongs to.
        int[] signedOut = { 0 };
        schoolUserRepo.findAllByUser_Id(userId).stream()
                .map(SchoolUser::getSchoolId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(schoolId -> sessionRepo.findAllByUserAndSchoolIdAndActive(userId, schoolId, true)
                        .stream()
                        .filter(session -> !session.getId().equals(currentSessionId))
                        .forEach(session -> {
                            session.deactivate();
                            sessionRepo.save(session);
                            signedOut[0]++;
                        }));
        return new Result(signedOut[0]);
    }
}
