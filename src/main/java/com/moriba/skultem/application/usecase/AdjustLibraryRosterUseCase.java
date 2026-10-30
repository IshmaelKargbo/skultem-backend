package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.LibraryRosterChange;
import com.moriba.skultem.domain.model.LibraryRosterChange.Change;
import com.moriba.skultem.domain.model.LibrarySession;
import com.moriba.skultem.domain.model.LibrarySessionRecord;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.LibraryRosterChangeRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRecordRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Adds a student to, or takes one off, a session's roster before it is finalized (D-15, B-15a). A reason is
 * required and every change is kept. A student taken off stays on the record as removed and isn't counted.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AdjustLibraryRosterUseCase {
    private final LibrarySessionRepository sessionRepo;
    private final LibrarySessionRecordRepository recordRepo;
    private final LibraryRosterChangeRepository changeRepo;
    private final StudentRepository studentRepo;
    private final AcademicYearRepository academicYearRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final LibraryAttendanceSupport support;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_ROSTER_STUDENT_ADDED")
    public void addStudent(String schoolId, String sessionId, String studentId, String reason, String userId) {
        var session = editableSession(schoolId, sessionId);
        requireReason(reason);

        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));
        var year = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("No active academic year found"));
        var enrollment = enrollmentRepo.findByStudentAndAcademicYearAndSchoolId(studentId, year.getId(), schoolId)
                .filter(e -> e.getStatus() == Enrollment.Status.ACTIVE)
                .orElseThrow(() -> new RuleException(student.getName() + " is not currently enrolled"));

        var existing = recordRepo.findBySessionAndStudent(sessionId, studentId, schoolId);
        LibrarySessionRecord record;
        if (existing.isPresent()) {
            record = existing.get();
            LibraryAttendanceSupport.rule(record::restoreToRoster);
        } else {
            record = LibrarySessionRecord.onRoster(schoolId, sessionId, studentId, student.getName(),
                    LibraryAttendanceSupport.className(enrollment), true);
        }
        recordRepo.save(record);
        saveChange(session, studentId, student.getName(), Change.ADDED, reason, userId);
    }

    @AuditLogAnnotation(action = "LIBRARY_ROSTER_STUDENT_REMOVED")
    public void removeStudent(String schoolId, String sessionId, String recordId, String reason, String userId) {
        var session = editableSession(schoolId, sessionId);
        requireReason(reason);

        var record = recordRepo.findByIdAndSchool(recordId, schoolId)
                .filter(r -> r.getSessionId().equals(sessionId))
                .orElseThrow(() -> new NotFoundException("Student is not on this session's roster"));
        LibraryAttendanceSupport.rule(record::removeFromRoster);
        recordRepo.save(record);
        saveChange(session, record.getStudentId(), record.getStudentName(), Change.REMOVED, reason, userId);
    }

    private LibrarySession editableSession(String schoolId, String sessionId) {
        var session = sessionRepo.findByIdAndSchoolForUpdate(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Library session not found"));
        LibraryAttendanceSupport.rule(() -> session.requireEditable(support.today()));
        return session;
    }

    private static void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new RuleException("A reason is required");
        }
    }

    private void saveChange(LibrarySession session, String studentId, String studentName, Change change,
            String reason, String userId) {
        changeRepo.save(LibraryRosterChange.record(session.getSchoolId(), session.getId(), studentId, studentName,
                change, reason, userId, support.userName(userId), support.now()));
        logActivityUseCase.log(session.getSchoolId(), ActivityType.LIBRARY,
                change == Change.ADDED ? "Added to library register" : "Removed from library register",
                studentName + " - " + session.getClassName() + " " + session.getDate(), null, session.getId());
    }
}
