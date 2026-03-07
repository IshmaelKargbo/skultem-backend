package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.LoginResponse;
import com.moriba.skultem.application.error.AccessDeniedException;
import com.moriba.skultem.domain.model.Role;
import com.moriba.skultem.domain.model.UserSession;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserSessionRepository;
import com.moriba.skultem.infrastructure.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final SchoolUserRepository schoolUserRepo;
    private final UserSessionRepository sessionRepo;
    private final JwtUtil jwt;

    public LoginResponse execute(String refreshToken) {

        String sessionId;
        try {
            if (!jwt.isRefreshToken(refreshToken)) {
                throw new AccessDeniedException("Not a refresh token");
            }
            sessionId = jwt.extractSessionIdFromRefresh(refreshToken);

            if (!jwt.validateRefreshToken(refreshToken, sessionId)) {
                throw new AccessDeniedException("Invalid refresh token");
            }
        } catch (Exception e) {
            throw new AccessDeniedException("Invalid refresh token");
        }

        UserSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new AccessDeniedException("Invalid session"));

        if (!session.isActive()) {
            throw new AccessDeniedException("Session expired or inactive");
        }

        String userId = session.getUser().getId();

        var schoolUsers = schoolUserRepo.findAllByUser_IdAndSchoolId(userId, session.getSchoolId());
        if (schoolUsers.isEmpty()) {
            throw new AccessDeniedException("User has no role in this school");
        }

        List<Role> roles = schoolUsers.stream().map(su -> su.getRole()).toList();

        String newAccessToken = jwt.generateAccessToken(userId, session.getSchoolId(), roles, sessionId);
        String newRefreshToken = jwt.generateRefreshToken(sessionId);

        return new LoginResponse(newAccessToken, newRefreshToken);
    }
}