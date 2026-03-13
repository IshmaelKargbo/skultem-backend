package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.UserSessionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class LogoutUseCase {

    private final UserSessionRepository sessionRepo;

    @AuditLogAnnotation(action = "LOGOUT")
    public void execute(String sessionId) {

        var session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found"));

        session.deactivate();

        sessionRepo.save(session);
    }
}