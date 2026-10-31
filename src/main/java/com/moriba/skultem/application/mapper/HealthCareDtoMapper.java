package com.moriba.skultem.application.mapper;

import java.time.Instant;

import com.moriba.skultem.application.dto.HealthCareReportDTOs.TurnedAway;
import com.moriba.skultem.application.dto.HealthCareSettingDTO;
import com.moriba.skultem.application.dto.HealthCareVisitDTO;
import com.moriba.skultem.domain.model.HealthCareSetting;
import com.moriba.skultem.domain.model.HealthCareTurnedAway;
import com.moriba.skultem.domain.model.HealthCareVisit;

public final class HealthCareDtoMapper {

    private HealthCareDtoMapper() {
    }

    public static HealthCareVisitDTO toDTO(HealthCareVisit v, HealthCareSetting setting, Instant now) {
        Long minutes = v.minutesIn(now);
        boolean longStay = v.isOpen() && !v.needsFollowUp(now) && setting.getLongStayMinutes() != null
                && minutes != null && minutes >= setting.getLongStayMinutes();
        boolean canNotify = v.isClosed() && (v.isNotifyRetry()
                || (setting.isAllowManualNotify() && !v.isParentsNotifiedByHand()
                        && v.getParentNotifications().isEmpty()));
        return new HealthCareVisitDTO(v.getId(), v.getStudentId(), v.getStudentName(), v.getStudentClass(),
                v.getStatus().name(), v.getArrivedAt(), v.getComplaints(), v.getComplaintOther(), v.getBroughtBy(),
                v.getTemperature(), v.getNotes(), v.getTreatment(), v.getMedications(), v.getContinuesVisitId(),
                v.getOverLimit(), v.getOpenedByName(), v.getOutcome() == null ? null : v.getOutcome().name(),
                v.getLeftAt(), v.getCollectedBy(), v.getCollectedByRelation(), v.getReferredTo(),
                v.getAmbulanceCalled(), v.getAccompaniedBy(), v.getClosedAt(), v.getClosedByName(),
                v.getFollowUpReason(), v.getFollowUpNotes(), v.getParentNotifications(), v.getNotifyProblem(),
                canNotify, v.getVoidedAt(), v.getVoidedByName(), v.getVoidReason(), v.isUrgent(),
                v.needsFollowUp(now), minutes, longStay);
    }

    public static HealthCareSettingDTO toDTO(HealthCareSetting s, boolean customized) {
        return new HealthCareSettingDTO(s.getLimitRules(), s.isMedicationAllowed(), s.getSameMedicineHours(),
                s.getSameMedicineAction().name(), s.getMaxMedicationLines(), s.isRequireBroughtBy(),
                s.isRequireTemperature(), s.isRequireTreatment(), s.isRequireNotesOnReturn(),
                s.getNotifyOutcomes().stream().map(Enum::name).sorted().toList(), s.isAllowManualNotify(),
                s.isNotifyPrincipalOnReferral(), s.getPrincipalAlertVisits(),
                s.getPrincipalAlertPeriod() == null ? null : s.getPrincipalAlertPeriod().name(),
                s.getLongStayMinutes(), s.getComplaints(), s.complaintList(), customized, s.getUpdatedByName(),
                customized ? s.getUpdatedAt() : null);
    }

    public static TurnedAway toDTO(HealthCareTurnedAway t) {
        return new TurnedAway(t.getId(), t.getStudentId(), t.getStudentName(), t.getStudentClass(), t.getAt(),
                t.getComplaints(), t.getRuleName(), t.getMaxVisits(), t.getPeriod(), t.getVisitCount(),
                t.getRecordedByName());
    }
}
