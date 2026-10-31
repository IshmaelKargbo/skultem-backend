package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.moriba.skultem.domain.model.HealthCareSetting;
import com.moriba.skultem.domain.model.HealthCareSetting.ComplaintSettings;
import com.moriba.skultem.domain.model.HealthCareSetting.LimitRule;
import com.moriba.skultem.domain.model.HealthCareSetting.Period;
import com.moriba.skultem.domain.model.HealthCareSetting.RepeatAction;
import com.moriba.skultem.domain.model.HealthCareVisit.Closing;
import com.moriba.skultem.domain.model.HealthCareVisit.Outcome;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Request bodies for HealthCareController (docs/HEALTH_CARE.md).
public final class HealthCareRequests {

    private HealthCareRequests() {
    }

    public record StartVisit(
            @NotBlank(message = "Choose a student") String studentId,
            Instant arrivedAt,
            @NotEmpty(message = "Choose at least one complaint") @Size(max = 10, message = "Choose at most 10 complaints") List<String> complaints,
            @Size(max = 500, message = "Keep the description under 500 characters") String complaintOther,
            @Size(max = 255, message = "Keep \"brought by\" under 255 characters") String broughtBy,
            BigDecimal temperature,
            @Size(max = 2000, message = "Keep the notes under 2000 characters") String notes,
            String continuesVisitId,
            @Size(max = 500, message = "Keep the reason under 500 characters") String overLimitReason) {
    }

    public record UpdateVisit(
            @NotEmpty(message = "Choose at least one complaint") @Size(max = 10, message = "Choose at most 10 complaints") List<String> complaints,
            @Size(max = 500, message = "Keep the description under 500 characters") String complaintOther,
            @Size(max = 255, message = "Keep \"brought by\" under 255 characters") String broughtBy,
            BigDecimal temperature,
            @Size(max = 2000, message = "Keep the notes under 2000 characters") String notes,
            @Size(max = 2000, message = "Keep the treatment under 2000 characters") String treatment) {
    }

    public record AddMedication(
            @NotBlank(message = "Enter the medicine's name") @Size(max = 120, message = "Keep the medicine's name under 120 characters") String name,
            @NotBlank(message = "Enter the dose given") @Size(max = 120, message = "Keep the dose under 120 characters") String dose,
            Instant givenAt,
            @Size(max = 500, message = "Keep the reason under 500 characters") String repeatReason) {
    }

    public record CloseVisit(
            @NotNull(message = "Choose how the visit ended") Outcome outcome,
            Instant leftAt,
            @Size(max = 2000, message = "Keep the treatment under 2000 characters") String treatment,
            @Size(max = 255) String collectedBy,
            @Size(max = 255) String collectedByRelation,
            @Size(max = 255) String referredTo,
            Boolean ambulanceCalled,
            @Size(max = 255) String accompaniedBy,
            @Size(max = 2000, message = "Keep the notes under 2000 characters") String notes,
            Boolean notifyParents,
            // Only for following up a visit nobody closed before the school day ended.
            @Size(max = 500, message = "Keep the reason under 500 characters") String reason) {

        public Closing toClosing() {
            return new Closing(outcome, leftAt, treatment, collectedBy, collectedByRelation, referredTo,
                    ambulanceCalled, accompaniedBy, notes);
        }
    }

    public record Text(
            @NotBlank(message = "Write the note first") @Size(max = 2000, message = "Keep the note under 2000 characters") String text) {
    }

    public record Reason(
            @NotBlank(message = "A reason is required") @Size(max = 500, message = "Keep the reason under 500 characters") String reason) {
    }

    public record Settings(
            List<LimitRule> limitRules,
            boolean medicationAllowed,
            Integer sameMedicineHours,
            RepeatAction sameMedicineAction,
            Integer maxMedicationLines,
            boolean requireBroughtBy,
            boolean requireTemperature,
            boolean requireTreatment,
            boolean requireNotesOnReturn,
            Set<Outcome> notifyOutcomes,
            boolean allowManualNotify,
            boolean notifyPrincipalOnReferral,
            Integer principalAlertVisits,
            Period principalAlertPeriod,
            Integer longStayMinutes,
            ComplaintSettings complaints) {

        public HealthCareSetting.Values toValues() {
            return new HealthCareSetting.Values(limitRules, medicationAllowed, sameMedicineHours, sameMedicineAction,
                    maxMedicationLines, requireBroughtBy, requireTemperature, requireTreatment, requireNotesOnReturn,
                    notifyOutcomes, allowManualNotify, notifyPrincipalOnReferral, principalAlertVisits,
                    principalAlertPeriod, longStayMinutes, complaints);
        }
    }
}
