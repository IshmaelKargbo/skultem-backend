package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

public record LibrarySessionDetailDTO(LibrarySessionDTO session, Instant openedAt, String openedByName,
        Instant finalizedAt, String finalizedByName, Instant cancelledAt, String cancelledByName,
        List<Record> records, List<RosterChange> rosterChanges, List<Correction> corrections) {

    public record Record(String id, String studentId, String studentName, String studentClass, String status,
            String excuseReason, boolean removed, boolean addedManually, boolean corrected) {
    }

    public record RosterChange(String studentId, String studentName, String change, String reason,
            Instant changedAt, String changedByName) {
    }

    public record Correction(String recordId, String studentId, String studentName, String originalStatus,
            String correctedStatus, String reason, Instant correctedAt, String correctedByName) {
    }
}
