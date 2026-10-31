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

/** Takes off a medication line entered by mistake, while the visit is still open. */
@Service
@Transactional
@RequiredArgsConstructor
public class RemoveHealthCareMedicationUseCase {
    private final HealthCareVisitRepository visitRepo;
    private final HealthCareSupport support;

    @AuditLogAnnotation(action = "HEALTH_CARE_MEDICATION_REMOVED", includeDetails = false)
    public HealthCareVisitDTO execute(String schoolId, String visitId, String medicationId, String userId) {
        var visit = visitRepo.findByIdAndSchoolForUpdate(visitId, schoolId)
                .orElseThrow(() -> new NotFoundException("Visit not found"));
        var now = support.now();
        LibraryAttendanceSupport.rule(() -> visit.removeMedication(medicationId, now));
        visitRepo.save(visit);
        return HealthCareDtoMapper.toDTO(visit, support.settings(schoolId), now);
    }
}
