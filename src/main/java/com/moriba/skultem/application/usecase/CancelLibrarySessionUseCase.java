package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.application.services.LibrarySessionSchedule;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.LibrarySession;
import com.moriba.skultem.domain.repository.LibrarySessionRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Cancels a scheduled library session, with a reason (D-6). A cancelled session counts no one as absent. It
 * can't be undone, and a finalized session can't be cancelled.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CancelLibrarySessionUseCase {
    private final LibrarySessionSchedule schedule;
    private final LibrarySessionRepository sessionRepo;
    private final LibraryAttendanceSupport support;
    private final LogActivityUseCase logActivityUseCase;

    /** A session that has been opened. */
    @AuditLogAnnotation(action = "LIBRARY_SESSION_CANCELLED")
    public String cancelOpened(String schoolId, String sessionId, String reason, String userId) {
        var session = sessionRepo.findByIdAndSchoolForUpdate(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Library session not found"));
        LibraryAttendanceSupport.rule(() -> session.cancel(userId, support.userName(userId), reason,
                support.today(), support.now()));
        sessionRepo.save(session);
        logCancelled(session);
        return session.getId();
    }

    /** A session nobody has opened yet, identified by its timetable entry and date. */
    @AuditLogAnnotation(action = "LIBRARY_SESSION_CANCELLED")
    public String cancelUpcoming(String schoolId, String timetableEntryId, LocalDate date, String reason,
            String userId) {
        var existing = sessionRepo.findByEntryAndDate(schoolId, timetableEntryId, date);
        if (existing.isPresent()) {
            return cancelOpened(schoolId, existing.get().getId(), reason, userId);
        }
        var slot = schedule.slotFor(schoolId, timetableEntryId, date);
        var session = LibraryAttendanceSupport.rule(() -> LibrarySession.cancelBeforeOpening(schoolId, slot, userId,
                support.userName(userId), reason, support.today(), support.now()));
        LibraryAttendanceSupport.saveUnique(() -> sessionRepo.save(session),
                "Someone else has just opened or cancelled this session. Reload the page to see it.");
        logCancelled(session);
        return session.getId();
    }

    private void logCancelled(LibrarySession session) {
        logActivityUseCase.log(session.getSchoolId(), ActivityType.LIBRARY, "Library session cancelled",
                session.getClassName() + " - " + session.getDate(), null, session.getId());
    }
}
