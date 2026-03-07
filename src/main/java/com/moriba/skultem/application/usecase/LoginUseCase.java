package com.moriba.skultem.application.usecase;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LoginResponse;
import com.moriba.skultem.application.error.AccessDeniedException;
import com.moriba.skultem.application.error.NotFoundException;
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

        private final UserRepository repo;
        private final SchoolRepository schoolRepo;
        private final SchoolUserRepository schoolUserRepo;
        private final UserSessionRepository sessionRepo;
        private final JwtUtil jwt;
        private final PasswordEncoder passwordEncoder;

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

                var school = schoolRepo.findByDomain(domain)
                                .orElseThrow(() -> new NotFoundException("School not found"));

                var user = repo.findByEmail(email)
                                .orElseThrow(() -> new AccessDeniedException("Invalid email or password"));

                if (!passwordEncoder.matches(password, user.getPassword())) {
                        throw new AccessDeniedException("Invalid email or password");
                }

                List<SchoolUser> schoolUsers = schoolUserRepo.findAllByUser_IdAndSchoolId(user.getId(), school.getId());

                if (schoolUsers.isEmpty()) {
                        throw new AccessDeniedException("Invalid email or password");
                }

                var roles = schoolUsers.stream()
                                .map(SchoolUser::getRole)
                                .toList();

                List<UserSession> activeSessions = sessionRepo.findAllByUserAndSchoolIdAndActive(user.getId(), school.getId(),
                                true);

                if (activeSessions.size() >= MAX_SESSIONS) {

                        UserSession oldestSession = activeSessions.stream()
                                        .min(Comparator.comparing(UserSession::getCreatedAt))
                                        .orElse(null);

                        if (oldestSession != null) {
                                oldestSession.deactivate();
                                sessionRepo.save(oldestSession);
                        }
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

                sessionRepo.save(session);

                String accessToken = jwt.generateAccessToken(
                                user.getId(),
                                school.getId(),
                                roles,
                                sessionId);

                String refreshToken = jwt.generateRefreshToken(sessionId);

                return new LoginResponse(accessToken, refreshToken);
        }
}