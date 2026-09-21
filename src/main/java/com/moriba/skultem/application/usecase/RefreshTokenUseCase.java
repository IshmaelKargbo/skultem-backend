package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.LoginResponse;
import com.moriba.skultem.application.error.AccessDeniedException;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.UserSession;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserSessionRepository;
import com.moriba.skultem.domain.vo.Role;
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

        // ACTIVE only, same as LoginUseCase - a role deactivated after this session was issued
        // (see SetUserAccessUseCase/RemoveRoleUseCase, which already kill the session outright)
        // must not keep getting silently re-granted every time the access token is refreshed.
        // A session with no school is a SYSTEM_ADMIN one (see SystemAdminLoginUseCase) - a
        // school-scoped lookup can't match a null school_id, so look up that membership directly.
        var memberships = session.getSchoolId() == null
                ? schoolUserRepo.findAllByUser_Id(userId).stream()
                        .filter(su -> su.getRole() == Role.SYSTEM_ADMIN && su.getSchoolId() == null)
                        .toList()
                : schoolUserRepo.findAllByUser_IdAndSchoolId(userId, session.getSchoolId());
        var schoolUsers = memberships.stream()
                .filter(su -> su.getStatus() == SchoolUser.Status.ACTIVE)
                .toList();

        if (schoolUsers.isEmpty()) {
            session.deactivate();
            sessionRepo.save(session);
            throw new AccessDeniedException("Your access to this school has been deactivated. Contact your school admin.");
        }

        List<Role> roles = schoolUsers.stream().map(su -> su.getRole()).toList();

        String newAccessToken = jwt.generateAccessToken(userId, session.getSchoolId(), roles, sessionId);
        String newRefreshToken = jwt.generateRefreshToken(sessionId);

        return new LoginResponse(newAccessToken, newRefreshToken);
    }
}