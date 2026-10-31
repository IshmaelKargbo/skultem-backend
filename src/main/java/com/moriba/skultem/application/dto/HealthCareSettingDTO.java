package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.model.HealthCareComplaint;
import com.moriba.skultem.domain.model.HealthCareSetting.ComplaintSettings;
import com.moriba.skultem.domain.model.HealthCareSetting.LimitRule;

/**
 * A school's health care rules (docs/HEALTH_CARE.md §6), plus the resulting complaint list ({@code complaintList},
 * hidden ones included and flagged) and whether the school has saved its own rules yet.
 */
public record HealthCareSettingDTO(List<LimitRule> limitRules, boolean medicationAllowed, Integer sameMedicineHours,
        String sameMedicineAction, Integer maxMedicationLines, boolean requireBroughtBy, boolean requireTemperature,
        boolean requireTreatment, boolean requireNotesOnReturn, List<String> notifyOutcomes,
        boolean allowManualNotify, boolean notifyPrincipalOnReferral, Integer principalAlertVisits,
        String principalAlertPeriod, Integer longStayMinutes, ComplaintSettings complaints,
        List<HealthCareComplaint> complaintList, boolean customized, String updatedByName, Instant updatedAt) {
}
