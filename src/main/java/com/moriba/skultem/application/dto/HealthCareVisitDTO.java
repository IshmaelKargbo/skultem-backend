package com.moriba.skultem.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.model.HealthCareVisit.Complaint;
import com.moriba.skultem.domain.model.HealthCareVisit.Medication;
import com.moriba.skultem.domain.model.HealthCareVisit.Note;
import com.moriba.skultem.domain.model.HealthCareVisit.OverLimit;
import com.moriba.skultem.domain.model.HealthCareVisit.ParentNotified;

/**
 * A sick bay visit with its clinical details - only ever sent to the nurse and principal-level users
 * (docs/HEALTH_CARE.md H-1). {@code needsFollowUp}: still open after the student's school day. {@code longStay}:
 * open longer than the school's long-stay alert.
 */
public record HealthCareVisitDTO(String id, String studentId, String studentName, String studentClass, String status,
        Instant arrivedAt, List<Complaint> complaints, String complaintOther, String broughtBy,
        BigDecimal temperature, String notes, String treatment, List<Medication> medications,
        String continuesVisitId, OverLimit overLimit, String openedByName, String outcome, Instant leftAt,
        String collectedBy, String collectedByRelation, String referredTo, Boolean ambulanceCalled,
        String accompaniedBy, Instant closedAt, String closedByName, String followUpReason, List<Note> followUpNotes,
        List<ParentNotified> parentNotifications, String notifyProblem, boolean canNotifyParents, Instant voidedAt,
        String voidedByName, String voidReason, boolean urgent, boolean needsFollowUp, Long minutesIn,
        boolean longStay) {
}
