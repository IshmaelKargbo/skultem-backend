package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HealthCareVisitDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.HealthCareDtoMapper;
import com.moriba.skultem.application.services.HealthCareSupport;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** Adds a follow-up note to a closed visit (V-8). Locked, so two notes at once both land. */
@Service
@Transactional
@RequiredArgsConstructor
public class AddHealthCareNoteUseCase {
    private final HealthCareVisitRepository visitRepo;
    private final HealthCareSupport support;

    @AuditLogAnnotation(action = "HEALTH_CARE_NOTE_ADDED", includeDetails = false)
    public HealthCareVisitDTO execute(String schoolId, String visitId, String text, String userId) {
        var visit = visitRepo.findByIdAndSchoolForUpdate(visitId, schoolId)
                .orElseThrow(() -> new NotFoundException("Visit not found"));
        var now = support.now();
        String userName = support.userName(userId);
        LibraryAttendanceSupport.rule(() -> visit.addNote(text, userId, userName, now));
        visitRepo.save(visit);
        return HealthCareDtoMapper.toDTO(visit, support.settings(schoolId), now);
    }
}
