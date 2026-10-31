package com.moriba.skultem.application.usecase;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HealthCareVisitDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.HealthCareDtoMapper;
import com.moriba.skultem.application.services.HealthCareSupport;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.HealthCareVisit;
import com.moriba.skultem.domain.model.HealthCareVisit.Closing;
import com.moriba.skultem.domain.model.HealthCareVisit.Outcome;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Ends a visit with an outcome (§5.3), or follows up one nobody closed before the school day ended (§5.4: a reason,
 * no departure time). Then, per the school's settings (§6.5, §7): tells the parents automatically for the chosen
 * outcomes, or when the nurse ticked "Notify parents" and the school allows it; and tells principal-level users
 * about a hospital referral. The visit is saved first, so a notification problem never undoes it (N-6).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CloseHealthCareVisitUseCase {
    private final HealthCareVisitRepository visitRepo;
    private final HealthCareSupport support;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "HEALTH_CARE_VISIT_CLOSED", includeDetails = false)
    public HealthCareVisitDTO close(String schoolId, String visitId, Closing closing, boolean notifyParents,
            String userId) {
        return run(schoolId, visitId, closing, notifyParents, null, userId);
    }

    @AuditLogAnnotation(action = "HEALTH_CARE_VISIT_FOLLOWED_UP", includeDetails = false)
    public HealthCareVisitDTO followUp(String schoolId, String visitId, Closing closing, String reason, String userId) {
        return run(schoolId, visitId, closing, false, reason == null ? "" : reason, userId);
    }

    private HealthCareVisitDTO run(String schoolId, String visitId, Closing closing, boolean notifyParents,
            String followUpReason, String userId) {
        // Locked, so two people closing the same visit at once can't both go through (V-6).
        var visit = visitRepo.findByIdAndSchoolForUpdate(visitId, schoolId)
                .orElseThrow(() -> new NotFoundException("Visit not found"));
        var setting = support.settings(schoolId);
        var now = support.now();
        String userName = support.userName(userId);

        if (followUpReason == null) {
            LibraryAttendanceSupport.rule(() -> visit.close(closing, setting.isRequireTreatment(),
                    setting.isRequireNotesOnReturn(), userId, userName, now));
        } else {
            LibraryAttendanceSupport.rule(() -> visit.followUp(closing, followUpReason, userId, userName, now));
        }
        visitRepo.save(visit);

        boolean auto = setting.notifiesParentsFor(visit.getOutcome());
        if (auto || (notifyParents && setting.isAllowManualNotify())) {
            var notified = support.notifyParents(visit, !auto, now);
            visit.recordNotified(notified.parents(), !auto, notified.problem(), now);
            visitRepo.save(visit);
        }
        if (visit.getOutcome() == Outcome.REFERRED && setting.isNotifyPrincipalOnReferral()) {
            alertReferral(visit);
        }

        logActivityUseCase.log(schoolId, ActivityType.HEALTH_CARE,
                followUpReason == null ? "Health care visit closed" : "Health care visit followed up",
                visit.getStudentName(), null, visit.getId());
        return HealthCareDtoMapper.toDTO(visit, setting, now);
    }

    private void alertReferral(HealthCareVisit visit) {
        support.notifyPrincipals(visit.getSchoolId(),
                visit.getStudentName() + " was referred to hospital",
                visit.getStudentName() + (visit.getStudentClass() == null ? "" : " (" + visit.getStudentClass() + ")")
                        + " was referred to " + visit.getReferredTo()
                        + (Boolean.TRUE.equals(visit.getAmbulanceCalled()) ? ". An ambulance was called." : "."),
                Map.of("student_id", visit.getStudentId(), "visit_id", visit.getId(), "kind", "referral"));
    }
}
