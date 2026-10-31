package com.moriba.skultem.infrastructure.persistence.mapper;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moriba.skultem.domain.model.HealthCareSetting;
import com.moriba.skultem.domain.model.HealthCareSetting.ComplaintSettings;
import com.moriba.skultem.domain.model.HealthCareSetting.LimitRule;
import com.moriba.skultem.domain.model.HealthCareSetting.Period;
import com.moriba.skultem.domain.model.HealthCareSetting.RepeatAction;
import com.moriba.skultem.domain.model.HealthCareTurnedAway;
import com.moriba.skultem.domain.model.HealthCareVisit;
import com.moriba.skultem.domain.model.HealthCareVisit.Complaint;
import com.moriba.skultem.domain.model.HealthCareVisit.Medication;
import com.moriba.skultem.domain.model.HealthCareVisit.Note;
import com.moriba.skultem.domain.model.HealthCareVisit.Outcome;
import com.moriba.skultem.domain.model.HealthCareVisit.OverLimit;
import com.moriba.skultem.domain.model.HealthCareVisit.ParentNotified;
import com.moriba.skultem.infrastructure.persistence.entity.HealthCareSettingEntity;
import com.moriba.skultem.infrastructure.persistence.entity.HealthCareTurnedAwayEntity;
import com.moriba.skultem.infrastructure.persistence.entity.HealthCareVisitEntity;

/** Health Care rows <-> domain. Lists that are only ever read with their row are stored as JSON on it. */
public final class HealthCareMapper {

    private HealthCareMapper() {
    }

    // Settings

    public static HealthCareSettingEntity toEntity(HealthCareSetting d) {
        return HealthCareSettingEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .limitRules(JsonMapper.toJson(d.getLimitRules()))
                .medicationAllowed(d.isMedicationAllowed())
                .sameMedicineHours(d.getSameMedicineHours())
                .sameMedicineAction(d.getSameMedicineAction().name())
                .maxMedicationLines(d.getMaxMedicationLines())
                .requireBroughtBy(d.isRequireBroughtBy())
                .requireTemperature(d.isRequireTemperature())
                .requireTreatment(d.isRequireTreatment())
                .requireNotesOnReturn(d.isRequireNotesOnReturn())
                .notifyOutcomes(JsonMapper.toJson(d.getNotifyOutcomes().stream().map(Enum::name).sorted().toList()))
                .allowManualNotify(d.isAllowManualNotify())
                .notifyPrincipalOnReferral(d.isNotifyPrincipalOnReferral())
                .principalAlertVisits(d.getPrincipalAlertVisits())
                .principalAlertPeriod(d.getPrincipalAlertPeriod() == null ? null : d.getPrincipalAlertPeriod().name())
                .longStayMinutes(d.getLongStayMinutes())
                .complaints(JsonMapper.toJson(d.getComplaints()))
                .updatedBy(d.getUpdatedBy())
                .updatedByName(d.getUpdatedByName())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    public static HealthCareSetting toDomain(HealthCareSettingEntity e) {
        List<LimitRule> rules = JsonMapper.fromJsonList(e.getLimitRules(), LimitRule.class);
        Set<Outcome> outcomes = JsonMapper.fromJsonStringList(e.getNotifyOutcomes()).stream().map(Outcome::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Outcome.class)));
        ComplaintSettings complaints = JsonMapper.fromJson(e.getComplaints(), ComplaintSettings.class);
        var values = new HealthCareSetting.Values(rules, e.isMedicationAllowed(), e.getSameMedicineHours(),
                RepeatAction.valueOf(e.getSameMedicineAction()), e.getMaxMedicationLines(), e.isRequireBroughtBy(),
                e.isRequireTemperature(), e.isRequireTreatment(), e.isRequireNotesOnReturn(), outcomes,
                e.isAllowManualNotify(), e.isNotifyPrincipalOnReferral(), e.getPrincipalAlertVisits(),
                e.getPrincipalAlertPeriod() == null ? null : Period.valueOf(e.getPrincipalAlertPeriod()),
                e.getLongStayMinutes(), complaints == null ? ComplaintSettings.empty() : withLists(complaints));
        return new HealthCareSetting(e.getId(), e.getSchoolId(), values, e.getUpdatedBy(), e.getUpdatedByName(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    // "{}" (the column default) reads back with null lists.
    private static ComplaintSettings withLists(ComplaintSettings c) {
        return new ComplaintSettings(c.hidden() == null ? List.of() : c.hidden(),
                c.urgent() == null ? List.of() : c.urgent(), c.custom() == null ? List.of() : c.custom());
    }

    // Visits

    public static HealthCareVisitEntity toEntity(HealthCareVisit d) {
        return HealthCareVisitEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .studentId(d.getStudentId())
                .studentName(d.getStudentName())
                .studentClass(d.getStudentClass())
                .status(d.getStatus())
                .arrivedAt(d.getArrivedAt())
                .schoolDayEndsAt(d.getSchoolDayEndsAt())
                .complaints(JsonMapper.toJson(d.getComplaints()))
                .complaintCodes("," + d.getComplaints().stream().map(Complaint::code).collect(Collectors.joining(","))
                        + ",")
                .complaintOther(d.getComplaintOther())
                .broughtBy(d.getBroughtBy())
                .temperature(d.getTemperature())
                .notes(d.getNotes())
                .treatment(d.getTreatment())
                .medications(JsonMapper.toJson(d.getMedications()))
                .continuesVisitId(d.getContinuesVisitId())
                .overLimit(d.getOverLimit() == null ? null : JsonMapper.toJson(d.getOverLimit()))
                .openedBy(d.getOpenedBy())
                .openedByName(d.getOpenedByName())
                .outcome(d.getOutcome())
                .leftAt(d.getLeftAt())
                .collectedBy(d.getCollectedBy())
                .collectedByRelation(d.getCollectedByRelation())
                .referredTo(d.getReferredTo())
                .ambulanceCalled(d.getAmbulanceCalled())
                .accompaniedBy(d.getAccompaniedBy())
                .closedAt(d.getClosedAt())
                .closedBy(d.getClosedBy())
                .closedByName(d.getClosedByName())
                .followUpReason(d.getFollowUpReason())
                .followUpNotes(JsonMapper.toJson(d.getFollowUpNotes()))
                .parentNotifications(JsonMapper.toJson(d.getParentNotifications()))
                .parentsNotifiedByHand(d.isParentsNotifiedByHand())
                .notifyProblem(d.getNotifyProblem())
                .voidedAt(d.getVoidedAt())
                .voidedBy(d.getVoidedBy())
                .voidedByName(d.getVoidedByName())
                .voidReason(d.getVoidReason())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    public static HealthCareVisit toDomain(HealthCareVisitEntity e) {
        return new HealthCareVisit(e.getId(), e.getSchoolId(), e.getStudentId(), e.getStudentName(),
                e.getStudentClass(), e.getStatus(), e.getArrivedAt(), e.getSchoolDayEndsAt(),
                JsonMapper.fromJsonList(e.getComplaints(), Complaint.class), e.getComplaintOther(), e.getBroughtBy(),
                e.getTemperature(), e.getNotes(), e.getTreatment(),
                JsonMapper.fromJsonList(e.getMedications(), Medication.class), e.getContinuesVisitId(),
                e.getOverLimit() == null ? null : JsonMapper.fromJson(e.getOverLimit(), new TypeReference<OverLimit>() {
                }), e.getOpenedBy(), e.getOpenedByName(), e.getOutcome(), e.getLeftAt(), e.getCollectedBy(),
                e.getCollectedByRelation(), e.getReferredTo(), e.getAmbulanceCalled(), e.getAccompaniedBy(),
                e.getClosedAt(), e.getClosedBy(), e.getClosedByName(), e.getFollowUpReason(),
                JsonMapper.fromJsonList(e.getFollowUpNotes(), Note.class),
                JsonMapper.fromJsonList(e.getParentNotifications(), ParentNotified.class),
                e.isParentsNotifiedByHand(), e.getNotifyProblem(), e.getVoidedAt(), e.getVoidedBy(),
                e.getVoidedByName(), e.getVoidReason(), e.getCreatedAt(), e.getUpdatedAt());
    }

    // Turned away

    public static HealthCareTurnedAwayEntity toEntity(HealthCareTurnedAway d) {
        return HealthCareTurnedAwayEntity.builder()
                .id(d.getId())
                .schoolId(d.getSchoolId())
                .studentId(d.getStudentId())
                .studentName(d.getStudentName())
                .studentClass(d.getStudentClass())
                .turnedAwayAt(d.getAt())
                .complaints(d.getComplaints())
                .ruleName(d.getRuleName())
                .maxVisits(d.getMaxVisits())
                .period(d.getPeriod())
                .visitCount(d.getVisitCount())
                .recordedBy(d.getRecordedBy())
                .recordedByName(d.getRecordedByName())
                .createdAt(d.getCreatedAt())
                .build();
    }

    public static HealthCareTurnedAway toDomain(HealthCareTurnedAwayEntity e) {
        return new HealthCareTurnedAway(e.getId(), e.getSchoolId(), e.getStudentId(), e.getStudentName(),
                e.getStudentClass(), e.getTurnedAwayAt(), e.getComplaints(), e.getRuleName(), e.getMaxVisits(),
                e.getPeriod(), e.getVisitCount(), e.getRecordedBy(), e.getRecordedByName(), e.getCreatedAt());
    }
}
