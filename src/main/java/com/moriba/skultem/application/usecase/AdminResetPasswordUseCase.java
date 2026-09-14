package com.moriba.skultem.application.usecase;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AdminResetPasswordResultDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.repository.UserSessionRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Role;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// An ADMIN/OWNER/PROPRIETOR issuing a brand new temporary password for a staff member - the
// "they forgot/lost their password and can't reach self-service" path. Mirrors how
// CreateUserUseCase sets a new account up (random temp password + Status.RESET_PASSWORD),
// except here the plaintext is handed back to the admin instead of emailed, so it can be
// shared directly (call, chat, in person). The staff member logs in with it and is routed to
// /reset-password exactly like a new hire is (see LoginUseCase / auth.global.ts), and the temp
// password is cleared the moment they set their own (see User.resetPassword).
@Service
@Transactional
@RequiredArgsConstructor
public class AdminResetPasswordUseCase {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$!";
    private static final int PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final UserSessionRepository sessionRepo;
    private final PasswordEncoder passwordEncoder;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "ADMIN_RESET_PASSWORD")
    public AdminResetPasswordResultDTO execute(String schoolId, String targetUserId, String actingUserId) {
        if (targetUserId.equals(actingUserId)) {
            throw new RuleException("Use your own account settings to change your password.");
        }

        var user = userRepo.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var memberships = schoolUserRepo.findAllByUser_IdAndSchoolId(targetUserId, schoolId);

        if (memberships.isEmpty()) {
            throw new NotFoundException("This user doesn't belong to your school.");
        }

        if (memberships.stream().anyMatch(m -> m.getRole() == Role.SYSTEM_ADMIN)) {
            throw new RuleException("System admin accounts can't be managed from here.");
        }

        var temporaryPassword = generatePassword();
        var passwordHash = passwordEncoder.encode(temporaryPassword);
        user.issueTemporaryPassword(passwordHash, temporaryPassword);
        userRepo.save(user);

        // Force a fresh login with the new temporary password - any session on the old one is
        // signed out immediately, same as SetUserAccessUseCase does on deactivation.
        sessionRepo.findAllByUserAndSchoolIdAndActive(targetUserId, schoolId, true)
                .forEach(session -> {
                    session.deactivate();
                    sessionRepo.save(session);
                });

        List<Role> roles = memberships.stream().map(m -> m.getRole()).toList();

        logActivityUseCase.log(
                schoolId,
                ActivityType.USER,
                "Password reset by admin",
                user.getGivenNames() + " " + user.getFamilyName(),
                null,
                user.getId());

        return new AdminResetPasswordResultDTO(UserMapper.toDTO(user, roles), temporaryPassword);
    }

    private String generatePassword() {
        StringBuilder value = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int index = RANDOM.nextInt(PASSWORD_CHARS.length());
            value.append(PASSWORD_CHARS.charAt(index));
        }
        return value.toString();
    }
}
