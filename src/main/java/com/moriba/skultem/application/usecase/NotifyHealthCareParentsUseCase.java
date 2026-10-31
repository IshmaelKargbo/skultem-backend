package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HealthCareVisitDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.HealthCareDtoMapper;
import com.moriba.skultem.application.services.HealthCareSupport;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Tells the parents about a closed visit by hand (N-2): once per visit, where the school allows it. Trying again
 * after an attempt that reached nobody is always allowed (N-6).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class NotifyHealthCareParentsUseCase {
    private final HealthCareVisitRepository visitRepo;
    private final HealthCareSupport support;

    @AuditLogAnnotation(action = "HEALTH_CARE_PARENTS_NOTIFIED", includeDetails = false)
    public HealthCareVisitDTO execute(String schoolId, String visitId, String userId) {
        var visit = visitRepo.findByIdAndSchoolForUpdate(visitId, schoolId)
                .orElseThrow(() -> new NotFoundException("Visit not found"));
        var setting = support.settings(schoolId);
        LibraryAttendanceSupport.rule(visit::requireManualNotifyAllowed);
        if (!setting.isAllowManualNotify() && !visit.isNotifyRetry()) {
            throw new RuleException("This school doesn't send parents a notification for this kind of visit");
        }
        var now = support.now();
        boolean manual = !setting.notifiesParentsFor(visit.getOutcome());
        var notified = support.notifyParents(visit, manual, now);
        visit.recordNotified(notified.parents(), manual, notified.problem(), now);
        visitRepo.save(visit);
        return HealthCareDtoMapper.toDTO(visit, setting, now);
    }
}
