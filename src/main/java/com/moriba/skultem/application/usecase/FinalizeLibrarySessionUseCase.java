package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.LibrarySessionRecordRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** Locks a session's register. Refused while any student on the roster is still unmarked (B-17, B-18). */
@Service
@Transactional
@RequiredArgsConstructor
public class FinalizeLibrarySessionUseCase {
    private final LibrarySessionRepository sessionRepo;
    private final LibrarySessionRecordRepository recordRepo;
    private final LibraryAttendanceSupport support;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_SESSION_FINALIZED")
    public void execute(String schoolId, String sessionId, String userId) {
        // Locked, so a mark saved at the same moment can't slip in after the check, and it finalizes once.
        var session = sessionRepo.findByIdAndSchoolForUpdate(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Library session not found"));
        var records = recordRepo.findBySession(sessionId, schoolId);

        LibraryAttendanceSupport.rule(() -> session.finalizeWith(records, userId, support.userName(userId),
                support.today(), support.now()));
        sessionRepo.save(session);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library register finalized",
                session.getClassName() + " - " + session.getDate(), null, session.getId());
    }
}
