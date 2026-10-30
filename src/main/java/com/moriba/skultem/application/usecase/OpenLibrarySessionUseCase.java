package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.application.services.LibraryRosterResolver;
import com.moriba.skultem.application.services.LibrarySessionSchedule;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.LibrarySession;
import com.moriba.skultem.domain.model.LibrarySessionRecord;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRecordRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Opens a scheduled library session to take the register (B-7, B-12). The session's details and its roster are
 * saved now, as they are today, and don't change afterwards when the timetable or the class does. Opening a
 * session that is already open just returns it.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OpenLibrarySessionUseCase {
    private final LibrarySessionSchedule schedule;
    private final LibrarySessionRepository sessionRepo;
    private final LibrarySessionRecordRepository recordRepo;
    private final ClassSessionRepository classSessionRepo;
    private final LibraryRosterResolver rosterResolver;
    private final LibraryAttendanceSupport support;
    private final LogActivityUseCase logActivityUseCase;

    /** Returns the session's id. */
    @AuditLogAnnotation(action = "LIBRARY_SESSION_OPENED")
    public String execute(String schoolId, String timetableEntryId, LocalDate date, String userId) {
        var existing = sessionRepo.findByEntryAndDate(schoolId, timetableEntryId, date);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        var slot = schedule.slotFor(schoolId, timetableEntryId, date);
        var classSession = classSessionRepo.findByIdAndSchoolId(slot.classSessionId(), schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        var session = LibraryAttendanceSupport.rule(() -> LibrarySession.open(schoolId, slot, userId,
                support.userName(userId), support.today(), support.now()));
        LibraryAttendanceSupport.saveUnique(() -> sessionRepo.save(session),
                "Someone else has just opened this session. Reload the page to see it.");

        var records = rosterResolver.roster(schoolId, classSession, slot.subjectId()).stream()
                .map(e -> LibrarySessionRecord.onRoster(schoolId, session.getId(), e.getStudent().getId(),
                        e.getStudent().getName(), LibraryAttendanceSupport.className(e), false))
                .toList();
        recordRepo.saveAll(records);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library session opened",
                session.getClassName() + " - " + date, null, session.getId());

        return session.getId();
    }
}
