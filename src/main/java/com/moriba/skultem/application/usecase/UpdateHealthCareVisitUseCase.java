package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.HealthCareVisitDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.HealthCareDtoMapper;
import com.moriba.skultem.application.services.HealthCareSupport;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.HealthCareVisit.Complaint;
import com.moriba.skultem.domain.repository.HealthCareVisitRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** Changes an open visit's details and treatment while the student is in the sick bay (§5.3). */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateHealthCareVisitUseCase {
    private final HealthCareVisitRepository visitRepo;
    private final HealthCareSupport support;

    public record Command(List<String> complaints, String complaintOther, String broughtBy, BigDecimal temperature,
            String notes, String treatment) {
    }

    @AuditLogAnnotation(action = "HEALTH_CARE_VISIT_UPDATED", includeDetails = false)
    public HealthCareVisitDTO execute(String schoolId, String visitId, Command c, String userId) {
        var visit = visitRepo.findByIdAndSchoolForUpdate(visitId, schoolId)
                .orElseThrow(() -> new NotFoundException("Visit not found"));
        var setting = support.settings(schoolId);
        StartHealthCareVisitUseCase.requireSettingFields(setting, c.broughtBy(), c.temperature());

        // A complaint already on the visit stays as it was chosen, even if the school has since hidden or renamed
        // it; anything new comes from the school's list as it is now.
        List<Complaint> complaints = new ArrayList<>();
        List<String> added = new ArrayList<>();
        for (String code : new LinkedHashSet<>(c.complaints() == null ? List.<String>of() : c.complaints())) {
            visit.getComplaints().stream().filter(k -> k.code().equals(code)).findFirst()
                    .ifPresentOrElse(complaints::add, () -> added.add(code));
        }
        if (!added.isEmpty()) {
            complaints.addAll(StartHealthCareVisitUseCase.resolveComplaints(setting, added));
        }

        var now = support.now();
        LibraryAttendanceSupport.rule(() -> visit.update(complaints, c.complaintOther(), c.broughtBy(),
                c.temperature(), c.notes(), c.treatment(), now));
        visitRepo.save(visit);
        return HealthCareDtoMapper.toDTO(visit, setting, now);
    }
}
