package com.moriba.skultem.application.dto;

import java.time.LocalDate;
import java.util.List;

/** A student's library record, with the two kinds kept apart - never added together. */
public record LibraryStudentHistoryDTO(String studentId, String studentName, List<LibraryVisitDTO> visits,
        List<Session> sessions) {

    // status is the student's current mark; sessionStatus whether the register was finalized or cancelled.
    public record Session(String sessionId, LocalDate date, String className, String periodName, String status,
            String sessionStatus, boolean corrected) {
    }
}
