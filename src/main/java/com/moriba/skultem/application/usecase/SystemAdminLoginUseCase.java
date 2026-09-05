package com.moriba.skultem.application.usecase;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LoginResponse;
import com.moriba.skultem.application.error.AccessDeniedException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AuditLog;
import com.moriba.skultem.domain.model.UserSession;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.repository.UserSessionRepository;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.security.JwtUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Sign-in for the system-admin portal (its own subdomain on the frontend - see auth.global.ts)
 * rather than any one school's login. Unlike LoginUseCase, this takes no school domain: a
 * SYSTEM_ADMIN isn't "of" a school the way every other role is (see
 * PermissionService#isSystemAdmin), so there's no tenant to resolve here - the account is looked
 * up by email/password alone, and this only succeeds if it holds a SYSTEM_ADMIN membership
 * *somewhere*. One of those membership's schoolId still anchors the UserSession/JWT underneath
 * (both need one, same as any login), but the portal itself never surfaces or depends on which.
 * <p>
 * Every failure path - unknown email, wrong password, an account with no SYSTEM_ADMIN membership
 * - throws the same generic message, so this can't be used to enumerate which emails exist or
 * which are (or aren't) system admins.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SystemAdminLoginUseCase {

    private static final int MAX_SESSIONS = 3;
    private static final String GENERIC_ERROR = "Invalid email or password";

    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserSessionRepository sessionRepository;
    private final AuditUseCase auditUseCase;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @AuditLogAnnotation(action = "SYSTEM_ADMIN_LOGIN_ATTEMPT")
    public LoginResponse execute(
            Credentials credentials,
            String ipAddress,
            String device,
            String deviceType,
            String os,
            String browser,
            String userAgent) {

        var user = userRepository.findByEmail(credentials.email())
                .orElseThrow(() -> new AccessDeniedException(GENERIC_ERROR));

        if (!passwordEncoder.matches(credentials.password(), user.getPassword())) {
            throw new AccessDeniedException(GENERIC_ERROR);
        }

        var systemAdminMembership = schoolUserRepository.findAllByUser_Id(user.getId()).stream()
                .filter(su -> su.getRole() == Role.SYSTEM_ADMIN)
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException(GENERIC_ERROR));

        String schoolId = systemAdminMembership.getSchoolId();

        List<UserSession> activeSessions = sessionRepository.findAllByUserAndSchoolIdAndActive(
                user.getId(), schoolId, true);

        if (activeSessions.size() >= MAX_SESSIONS) {
            deactivateOldestSession(activeSessions);
        }

        String sessionId = UUID.randomUUID().toString();

        UserSession session = UserSession.create(
                sessionId, schoolId, user, ipAddress, device, deviceType, os, browser, userAgent);

        sessionRepository.save(session);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), schoolId, List.of(Role.SYSTEM_ADMIN),
                sessionId);
        String refreshToken = jwtUtil.generateRefreshToken(sessionId);

        auditUseCase.log("SYSTEM_ADMIN_LOGIN_SUCCESS", user.getId(), schoolId, AuditLog.Status.SUCCESS,
                "System admin logged in: " + user.getEmail());

        return new LoginResponse(accessToken, refreshToken);
    }

    private void deactivateOldestSession(List<UserSession> sessions) {
        sessions.stream()
                .min(Comparator.comparing(UserSession::getCreatedAt))
                .ifPresent(session -> {
                    session.deactivate();
                    sessionRepository.save(session);
                });
    }

    /**
     * Deliberately not a record and deliberately no {@code toString()} override - AuditAspect logs
     * every argument of an {@code @AuditLogAnnotation} method verbatim via its default
     * {@code toString()} (see generateDetails), which would put the plaintext password straight
     * into the audit log if this exposed its fields the way a record's generated toString would.
     * The JVM's default {@code Object#toString()} (class name + hash) keeps that out while still
     * letting the annotation cover both success and failure attempts here - unlike
     * BootstrapSystemAdminUseCase, which skips the annotation entirely because it has no such safe
     * way to keep the token/password out of a per-argument log line. LoginUseCase has this same
     * plaintext-password-in-audit-log gap on its own password parameter; not fixed here, out of
     * scope for this change.
     */
    public static final class Credentials {
        private final String email;
        private final String password;

        public Credentials(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String email() {
            return email;
        }

        public String password() {
            return password;
        }
    }
}
