package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.LibrarySession;
import com.moriba.skultem.domain.model.LibrarySessionRecord;
import com.moriba.skultem.domain.repository.LibrarySessionRecordRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRepository;
import com.moriba.skultem.domain.repository.LibraryVisitRepository;
import com.moriba.skultem.domain.shared.SchoolTimeZone;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Locks a session's register. Refused while any student on the roster is still unmarked (B-17, B-18).
 * <p>
 * Finalizing also checks out every student on the roster who still has an open library visit from the
 * session's day, so nobody is left checked in once the library session is over.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class FinalizeLibrarySessionUseCase {
    private final LibrarySessionRepository sessionRepo;
    private final LibrarySessionRecordRepository recordRepo;
    private final LibraryVisitRepository visitRepo;
    private final LibraryAttendanceSupport support;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_SESSION_FINALIZED")
    public void execute(String schoolId, String sessionId, String userId) {
        // Locked, so a mark saved at the same moment can't slip in after the check, and it finalizes once.
        var session = sessionRepo.findByIdAndSchoolForUpdate(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Library session not found"));
        var records = recordRepo.findBySession(sessionId, schoolId);
        var userName = support.userName(userId);
        var now = support.now();

        LibraryAttendanceSupport.rule(() -> session.finalizeWith(records, userId, userName,
                support.today(), now));
        sessionRepo.save(session);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library register finalized",
                session.getClassName() + " - " + session.getDate(), null, session.getId());

        int checkedOut = checkOutRoster(schoolId, session, records, userId, userName, now);
        if (checkedOut > 0) {
            logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library check-out",
                    checkedOut + " student" + (checkedOut == 1 ? "" : "s") + " - " + session.getClassName(),
                    null, session.getId());
        }
    }

    /**
     * Checks out the roster's open visits. Only visits checked in on the session's day: an older open visit
     * is a forgotten check-out and is left for someone to resolve, rather than given a made-up duration.
     */
    private int checkOutRoster(String schoolId, LibrarySession session, List<LibrarySessionRecord> records,
            String userId, String userName, Instant now) {
        var studentIds = records.stream().filter(LibrarySessionRecord::isCounted)
                .map(LibrarySessionRecord::getStudentId).distinct().toList();
        LocalDate day = session.getDate();
        int count = 0;
        for (var open : visitRepo.findOpenByStudents(schoolId, studentIds)) {
            if (!day.equals(LocalDate.ofInstant(open.getCheckedInAt(), SchoolTimeZone.ZONE))) {
                continue;
            }
            // Re-read under a lock, so a librarian checking the same student out right now can't double it.
            var visit = visitRepo.findByIdAndSchoolForUpdate(open.getId(), schoolId).orElse(null);
            if (visit == null || !visit.isOpen()) {
                continue;
            }
            visit.checkOut(userId, userName, now);
            visitRepo.save(visit);
            count++;
        }
        return count;
    }
}
