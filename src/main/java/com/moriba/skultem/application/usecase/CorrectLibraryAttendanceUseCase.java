package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.LibraryAttendanceCorrection;
import com.moriba.skultem.domain.model.LibrarySessionRecord.Status;
import com.moriba.skultem.domain.repository.LibraryAttendanceCorrectionRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRecordRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Changes one mark on a finalized session (§7). Only principal-level roles reach this (the controller's gate).
 * The original and new status, who, when and why are kept on a correction record that is never edited.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CorrectLibraryAttendanceUseCase {
    private final LibrarySessionRepository sessionRepo;
    private final LibrarySessionRecordRepository recordRepo;
    private final LibraryAttendanceCorrectionRepository correctionRepo;
    private final LibraryAttendanceSupport support;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_ATTENDANCE_CORRECTED")
    public void execute(String schoolId, String sessionId, String recordId, Status newStatus, String excuseReason,
            String reason, String userId) {
        if (reason == null || reason.isBlank()) {
            throw new RuleException("A reason is required for a correction");
        }
        var session = sessionRepo.findByIdAndSchoolForUpdate(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Library session not found"));
        LibraryAttendanceSupport.rule(session::requireCorrectable);

        var record = recordRepo.findByIdAndSchool(recordId, schoolId)
                .filter(r -> r.getSessionId().equals(sessionId))
                .orElseThrow(() -> new NotFoundException("Student is not on this session's roster"));

        var now = support.now();
        Status original = LibraryAttendanceSupport.rule(() -> record.correct(newStatus, excuseReason, userId, now));
        var correction = LibraryAttendanceCorrection.of(record, original, reason, userId, support.userName(userId),
                now);
        recordRepo.save(record);
        correctionRepo.save(correction);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library attendance corrected",
                record.getStudentName() + ": " + original.name() + " to " + newStatus.name() + " ("
                        + session.getClassName() + " " + session.getDate() + ")",
                null, session.getId());
    }
}
