package com.moriba.skultem.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.LibrarySessionRecord;
import com.moriba.skultem.domain.model.LibrarySessionRecord.Status;
import com.moriba.skultem.domain.repository.LibrarySessionRecordRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** Saves marks on a session that is in progress. Each student is marked by hand; nothing is filled in for them. */
@Service
@Transactional
@RequiredArgsConstructor
public class MarkLibrarySessionUseCase {
    private final LibrarySessionRepository sessionRepo;
    private final LibrarySessionRecordRepository recordRepo;
    private final LibraryAttendanceSupport support;

    public record Mark(String recordId, Status status, String excuseReason) {
    }

    @AuditLogAnnotation(action = "LIBRARY_SESSION_MARKED")
    public void execute(String schoolId, String sessionId, List<Mark> marks, String userId) {
        if (marks == null || marks.isEmpty()) {
            throw new BadRequestException("Nothing to save");
        }
        var session = sessionRepo.findByIdAndSchoolForUpdate(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Library session not found"));
        LibraryAttendanceSupport.rule(() -> session.requireEditable(support.today()));

        Map<String, LibrarySessionRecord> byId = recordRepo.findBySession(sessionId, schoolId).stream()
                .collect(Collectors.toMap(LibrarySessionRecord::getId, Function.identity()));
        var now = support.now();
        for (Mark mark : marks) {
            var record = byId.get(mark.recordId());
            if (record == null) {
                throw new NotFoundException("Student is not on this session's roster");
            }
            LibraryAttendanceSupport.rule(() -> record.mark(mark.status(), mark.excuseReason(), userId, now));
        }
        recordRepo.saveAll(byId.values());
    }
}
