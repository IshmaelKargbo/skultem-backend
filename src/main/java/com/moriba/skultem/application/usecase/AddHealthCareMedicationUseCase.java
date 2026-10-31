package com.moriba.skultem.application.usecase;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HealthCareVisitDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.HealthCareDtoMapper;
import com.moriba.skultem.application.services.HealthCareSupport;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.HealthCareSetting.RepeatAction;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Records a medicine given during an open visit (§5.3, §6.3). Refused when the school gives no medication (M-1) or
 * the visit has its maximum lines. The same medicine again within the school's window only warns or asks for a
 * reason - it's never refused, because the nurse may know something the system doesn't (M-2).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AddHealthCareMedicationUseCase {
    private final HealthCareVisitRepository visitRepo;
    private final HealthCareSupport support;

    public record Command(String name, String dose, Instant givenAt, String repeatReason) {
    }

    @AuditLogAnnotation(action = "HEALTH_CARE_MEDICATION_RECORDED", includeDetails = false)
    public HealthCareVisitDTO execute(String schoolId, String visitId, Command c, String userId) {
        var setting = support.settings(schoolId);
        if (!setting.isMedicationAllowed()) {
            throw new RuleException("This school doesn't give medication in the sick bay");
        }
        var visit = visitRepo.findByIdAndSchoolForUpdate(visitId, schoolId)
                .orElseThrow(() -> new NotFoundException("Visit not found"));
        if (setting.getMaxMedicationLines() != null && visit.getMedications().size() >= setting.getMaxMedicationLines()) {
            throw new RuleException("A visit can have at most " + setting.getMaxMedicationLines() + " medication "
                    + (setting.getMaxMedicationLines() == 1 ? "line" : "lines"));
        }
        var now = support.now();
        if (setting.getSameMedicineHours() != null && setting.getSameMedicineAction() == RepeatAction.REASON
                && (c.repeatReason() == null || c.repeatReason().isBlank())) {
            support.lastSameMedicine(schoolId, visit.getStudentId(), c.name(), setting.getSameMedicineHours(), now)
                    .ifPresent(last -> {
                        throw new RuleException(c.name().trim() + " was already given " + ago(last.givenAt(), now)
                                + ". Give a reason to record it again.");
                    });
        }
        String userName = support.userName(userId);
        LibraryAttendanceSupport.rule(() -> visit.addMedication(c.name(), c.dose(), c.givenAt(), c.repeatReason(),
                userId, userName, now));
        visitRepo.save(visit);
        return HealthCareDtoMapper.toDTO(visit, setting, now);
    }

    static String ago(Instant when, Instant now) {
        long minutes = Math.max(0, Duration.between(when, now).toMinutes());
        if (minutes < 60) {
            return minutes + " min ago";
        }
        long hours = minutes / 60;
        long rest = minutes % 60;
        return hours + " h" + (rest == 0 ? "" : " " + rest + " min") + " ago";
    }
}
