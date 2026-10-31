package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HealthCareVisitDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.HealthCareDtoMapper;
import com.moriba.skultem.application.services.HealthCareSupport;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** Voids a visit recorded by mistake (V-10): kept, left out of reports and limits, never restored. */
@Service
@Transactional
@RequiredArgsConstructor
public class VoidHealthCareVisitUseCase {
    private final HealthCareVisitRepository visitRepo;
    private final HealthCareSupport support;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "HEALTH_CARE_VISIT_VOIDED", includeDetails = false)
    public HealthCareVisitDTO execute(String schoolId, String visitId, String reason, String userId) {
        var visit = visitRepo.findByIdAndSchoolForUpdate(visitId, schoolId)
                .orElseThrow(() -> new NotFoundException("Visit not found"));
        var now = support.now();
        String userName = support.userName(userId);
        LibraryAttendanceSupport.rule(() -> visit.voidVisit(reason, userId, userName, now));
        visitRepo.save(visit);
        logActivityUseCase.log(schoolId, ActivityType.HEALTH_CARE, "Health care visit voided", visit.getStudentName(),
                null, visit.getId());
        return HealthCareDtoMapper.toDTO(visit, support.settings(schoolId), now);
    }
}
