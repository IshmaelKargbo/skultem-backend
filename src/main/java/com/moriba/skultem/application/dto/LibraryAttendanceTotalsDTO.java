package com.moriba.skultem.application.dto;

import java.time.LocalDate;
import java.util.List;

/** Present / Absent / Excused counts from finalized sessions only, per class and overall. */
public record LibraryAttendanceTotalsDTO(LocalDate from, LocalDate to, List<Row> classes, Row overall,
        int incompleteSessions, int cancelledSessions) {

    public record Row(String classSessionId, String className, int sessions, int present, int absent,
            int excused) {
    }
}
