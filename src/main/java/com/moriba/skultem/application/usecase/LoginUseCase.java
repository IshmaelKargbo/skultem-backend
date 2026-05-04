package com.moriba.skultem.application.usecase;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LoginResponse;
import com.moriba.skultem.application.error.AccessDeniedException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AuditLog;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.UserSession;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.repository.UserSessionRepository;
import com.moriba.skultem.infrastructure.security.JwtUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class LoginUseCase {

        private static final int MAX_SESSIONS = 3;

        private final UserRepository userRepository;
        private final SchoolRepository schoolRepository;
        private final SchoolUserRepository schoolUserRepository;
        private final UserSessionRepository sessionRepository;
        private final AuditUseCase auditUseCase;
        private final JwtUtil jwtUtil;
        private final PasswordEncoder passwordEncoder;

        @AuditLogAnnotation(action = "LOGIN_ATTEMPT")
        public LoginResponse execute(
                        String domain,
                        String email,
                        String password,
                        String ipAddress,
                        String device,
                        String deviceType,
                        String os,
                        String browser,
                        String userAgent) {

                var school = schoolRepository.findByDomain(domain)
                                .orElseThrow(() -> new NotFoundException("School not found"));

                var user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new AccessDeniedException("Invalid email"));

                if (!passwordEncoder.matches(password, user.getPassword())) {
                        throw new AccessDeniedException("Invalid password");
                }

                List<SchoolUser> schoolUsers = schoolUserRepository.findAllByUser_IdAndSchoolId(user.getId(),
                                school.getId());

                if (schoolUsers.isEmpty()) {
                        throw new AccessDeniedException("Invalid email or password");
                }

                var roles = schoolUsers.stream()
                                .map(SchoolUser::getRole)
                                .distinct()
                                .toList();

                List<UserSession> activeSessions = sessionRepository.findAllByUserAndSchoolIdAndActive(
                                user.getId(),
                                school.getId(),
                                true);

                if (activeSessions.size() >= MAX_SESSIONS) {
                        deactivateOldestSession(activeSessions);
                }

                String sessionId = UUID.randomUUID().toString();

                UserSession session = UserSession.create(
                                sessionId,
                                school.getId(),
                                user,
                                ipAddress,
                                device,
                                deviceType,
                                os,
                                browser,
                                userAgent);

                sessionRepository.save(session);

                String accessToken = jwtUtil.generateAccessToken(
                                user.getId(),
                                school.getId(),
                                roles,
                                sessionId);

                String refreshToken = jwtUtil.generateRefreshToken(sessionId);

                auditUseCase.log("LOGIN_SUCCESS", user.getId(), school.getId(), AuditLog.Status.SUCCESS,
                                "User logged in: " + user.getEmail());

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
}